package com.pinterest.secor.parser;

import com.google.common.primitives.Longs;
import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.message.Message;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDecoder;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;

public class ConfluentAvroMessageParser extends TimestampedMessageParser {
    private final String timestampFieldName;
    private final CachedSchemaRegistryClient srClient;
    private final KafkaAvroDecoder avroDecoder;

    public ConfluentAvroMessageParser(SecorConfig config) throws IOException {
        super(config);
        String confluentUrl = mConfig.getConfluentSchemaRegistryURL();
        this.srClient = new CachedSchemaRegistryClient(confluentUrl, 10);
        this.timestampFieldName = config.getMessageTimestampName();
        this.avroDecoder = new KafkaAvroDecoder(srClient);
    }

    @Override
    public long extractTimestampMillis(Message message) throws Exception {
        // Fetch the payload and deserialize to get the timestamp.
        byte[] payload = message.getPayload();
        if (payload == null || payload.length == 0) {
            return 0L;
        }

        GenericRecord record = (GenericRecord) avroDecoder.fromBytes(payload);
        Object val = record.get(timestampFieldName);
        Long longVal = Longs.tryParse(String.valueOf(val));
        if (longVal == null) {
            longVal = 0L;
        }
        return toMillis(longVal);
    }
}