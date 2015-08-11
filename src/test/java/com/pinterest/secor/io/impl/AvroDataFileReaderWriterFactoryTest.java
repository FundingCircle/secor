package com.pinterest.secor.io.impl;

import com.google.common.io.Files;
import com.pinterest.secor.common.LogFilePath;
import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.io.FileReader;
import com.pinterest.secor.io.FileWriter;
import com.pinterest.secor.io.KeyValue;
import com.pinterest.secor.message.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SecorConfig.class)
@PowerMockIgnore({"javax.xml.parsers.*", "com.sun.org.apache.xerces.internal.*", "org.apache.*"})
public class AvroDataFileReaderWriterFactoryTest {

    @Test
    public void testRoundTrip() throws Exception {
        // Write an Avro data file using the FileWriter, and then verify that
        // it can be read by the FileReader.
        String schemaText = "{\"namespace\": \"example.avro\",\n" +
                " \"type\": \"record\",\n" +
                " \"name\": \"User\",\n" +
                " \"fields\": [\n" +
                "     {\"name\": \"name\", \"type\": \"string\"},\n" +
                "     {\"name\": \"timestamp\", \"type\": [\"long\", \"null\", \"string\"]},\n" +
                "     {\"name\": \"favorite_number\",  \"type\": [\"int\", \"null\"]},\n" +
                "     {\"name\": \"favorite_color\", \"type\": [\"string\", \"null\"]}\n" +
                " ]\n" +
                "}";
        Path avroFile = java.nio.file.Files.createTempFile("foo", "avro");
        FileUtils.writeStringToFile(avroFile.toFile(), schemaText);
        Schema schema = new Schema.Parser().parse(avroFile.toFile());
        SecorConfig config = Mockito.mock(SecorConfig.class);
        PowerMockito.mockStatic(SecorConfig.class);
        PowerMockito.when(SecorConfig.load()).thenReturn(config);
        Mockito.when(config.getMessageTimestampName()).thenReturn("timestamp");
        Mockito.when(config.getMessageInputAvroSchema()).thenReturn(avroFile.toAbsolutePath().toString());


        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("name", "able-baker-1");
        user1.put("timestamp", 123L);
        user1.put("favorite_number", 42);

        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();

        BinaryEncoder encoder = EncoderFactory.get().blockingBinaryEncoder(baos1, null);
        datumWriter.write(user1, encoder);
        encoder.flush();
        Message msg1 = new Message("test-topic", 0, 0, baos1.toByteArray());

        AvroDataFileReaderWriterFactory factory = new AvroDataFileReaderWriterFactory();
        LogFilePath tempLogFilePath = new LogFilePath(Files.createTempDir().toString(),
                "test-topic",
                new String[]{"part-1"},
                0,
                1,
                0,
                ".log"
        );
        FileWriter writer = factory.BuildFileWriter(tempLogFilePath, null);
        KeyValue kv1 = (new KeyValue(1, msg1.getPayload()));
        writer.write(kv1);
        writer.close();
        FileReader fileReader = factory.BuildFileReader(tempLogFilePath, null);

        KeyValue kvout = fileReader.next();
        byte[] outValue = kvout.getValue();
        assertNotNull(outValue);
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(outValue, null);
        GenericRecord outRecord = datumReader.read(null, decoder);
        assertNotNull(outRecord);
        assertEquals("able-baker-1", outRecord.get("name").toString());
        assertEquals(123L, outRecord.get("timestamp"));
        assertEquals(42, outRecord.get("favorite_number"));
    }
}
