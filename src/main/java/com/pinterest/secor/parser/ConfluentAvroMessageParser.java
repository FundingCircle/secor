package com.pinterest.secor.parser;

import com.google.common.primitives.Longs;
import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.message.Message;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Avro message parser extracts date partitions from avro messages.
 * Uses: 'message.timestamp.name' to identify the field name.
 */
public class ConfluentAvroMessageParser extends TimestampedMessageParser {
    private final String timestampFieldName;
    private CachedSchemaRegistryClient srClient;
    private ConcurrentHashMap<String, GenericDatumReader<GenericRecord>> readerCache =
            new ConcurrentHashMap<String, GenericDatumReader<GenericRecord>>();

    public ConfluentAvroMessageParser(SecorConfig config) throws IOException {
        super(config);
        String confluentUrl = mConfig.getConfluentSchemaRegistryURL();
        this.srClient = new CachedSchemaRegistryClient(confluentUrl, 1);
        timestampFieldName = config.getMessageTimestampName();
    }

    @Override
    public long extractTimestampMillis(Message message) throws Exception {
        // We cache by the schema registry subject associated with each topic.
        // This is just the topic name plus "-value".
        String subjectKey = message.getTopic() + "-value";
        GenericDatumReader<GenericRecord> reader = readerCache.get(subjectKey);
        if (reader == null) {
            // If not in the cache, fetch from our SchemaRegistryClient.
            // Note that the client caches as well, so this should be cheap.
            // Also note that we don't care about the race condition here -
            // fetching the schema twice isn't a big deal.
            Schema schema = new Schema.Parser().parse(srClient.getLatestSchemaMetadata(subjectKey).getSchema());
            reader = new GenericDatumReader<GenericRecord>(schema);
            readerCache.put(subjectKey, reader);
        }

        // Fetch the payload and deserialize to get the timestamp.
        byte[] payload = message.getPayload();
        if (payload == null || payload.length == 0) {
            return 0L;
        }

        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
        GenericRecord record = reader.read(null, decoder);
        Object val = record.get(timestampFieldName);
        Long longVal = Longs.tryParse(String.valueOf(val));
        if (longVal == null) {
            longVal = 0L;
        }
        return toMillis(longVal);
    }
}