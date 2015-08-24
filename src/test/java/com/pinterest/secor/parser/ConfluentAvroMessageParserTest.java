package com.pinterest.secor.parser;

import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.message.Message;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConfluentAvroMessageParser.class)
public class ConfluentAvroMessageParserTest {
    @Test
    public void testExtractTimestampMillis() throws Exception {
        // Define a schema.
        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"WikipediaRaw\",\"namespace\":\"org.apache.avro.ipc\",\"fields\":[{\"name\":\"raw\",\"type\":[\"null\",\"string\"]},{\"name\":\"source\",\"type\":[\"null\",\"string\"]},{\"name\":\"channel\",\"type\":[\"null\",\"string\"]},{\"name\":\"timestamp\",\"type\":\"long\"}]}");

        // Create mocks and fakes.
        SecorConfig mockConfig = Mockito.mock(SecorConfig.class);
        CachedSchemaRegistryClient mockClient = Mockito.mock(CachedSchemaRegistryClient.class);
        SchemaMetadata fakeSchemaMetadata = new SchemaMetadata(1, 1, schema.toString());

        // Setup mock behaviors.
        Mockito.when(mockConfig.getConfluentSchemaRegistryURL()).thenReturn("http://localhost:8081");
        Mockito.when(mockConfig.getMessageTimestampName()).thenReturn("timestamp");
        PowerMockito.whenNew(CachedSchemaRegistryClient.class).withAnyArguments().thenReturn(mockClient);
        Mockito.when(mockClient.getLatestSchemaMetadata("test-topic-value")).thenReturn(fakeSchemaMetadata);
        Mockito.when(mockClient.getByID((int) Mockito.anyInt())).thenReturn(schema);

        // Generate and serialize our test Message.
        GenericRecord record = new GenericRecordBuilder(schema)
                .set("raw", "This is raw")
                .set("source", "This is source")
                .set("channel", "This is channel")
                .set("timestamp", 1440116996000L)
                .build();
        KafkaAvroSerializer serializer = new KafkaAvroSerializer(mockClient);
        byte[] payload = serializer.serialize("test-topic", record);
        Message fakeMessage = new Message("test-topic", 0, 0, payload);

        // Execute method under test.
        ConfluentAvroMessageParser parser = new ConfluentAvroMessageParser(mockConfig);
        long result = parser.extractTimestampMillis(fakeMessage);

        // Assert results.
        assertEquals(1440116996000L, result);
    }
}
