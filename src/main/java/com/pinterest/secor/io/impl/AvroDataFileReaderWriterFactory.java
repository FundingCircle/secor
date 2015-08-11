package com.pinterest.secor.io.impl;


import com.google.common.io.CountingOutputStream;
import com.pinterest.secor.common.LogFilePath;
import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.io.FileReader;
import com.pinterest.secor.io.FileReaderWriterFactory;
import com.pinterest.secor.io.FileWriter;
import com.pinterest.secor.io.KeyValue;
import com.pinterest.secor.util.FileUtil;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.mapred.FsInput;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.Compressor;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AvroDataFileReaderWriterFactory implements FileReaderWriterFactory {
    @Override
    public FileReader BuildFileReader(LogFilePath path, CompressionCodec codec) throws Exception {
        return new AvroDataFileReader(path);
    }

    @Override
    public FileWriter BuildFileWriter(LogFilePath path, CompressionCodec codec) throws Exception {
        return new AvroDataFileWriter(path, codec);
    }

    /**
     * I'm not really happy with this implementation - a Reader shouldn't
     * require a DatumWriter! - but it solves the immediate problem of
     * reading/writing Avro object container files.
     */
    protected class AvroDataFileReader implements FileReader {

        private final DataFileReader<GenericRecord> innerReader;
        private final GenericDatumReader<GenericRecord> datumReader;
        private final GenericDatumWriter<GenericRecord> datumWriter;
        private GenericRecord nextValue = null;

        public AvroDataFileReader(LogFilePath path) throws IOException, ConfigurationException {
            Path fsPath = new Path(path.getLogFilePath());
            FileSystem fs = FileUtil.getFileSystem(path.getLogFilePath());
            FsInput fileInput = new FsInput(fsPath, fs.getConf());
            File schemaFile = new File(SecorConfig.load().getMessageInputAvroSchema());
            Schema schema = new Schema.Parser().parse(schemaFile);
            this.datumReader = new GenericDatumReader<GenericRecord>(schema);
            this.datumWriter = new GenericDatumWriter<GenericRecord>(schema);
            this.innerReader = new DataFileReader<GenericRecord>(fileInput, datumReader);
        }

        @Override
        public KeyValue next() throws IOException {
            nextValue = innerReader.next();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder enc = EncoderFactory.get().binaryEncoder(out, null);
            datumWriter.write(nextValue, enc);
            enc.flush();
            out.close();
            return new KeyValue(0L, out.toByteArray());
        }

        @Override
        public void close() throws IOException {
            innerReader.close();
        }
    }

    protected class AvroDataFileWriter implements FileWriter {
        private final DataFileWriter innerWriter;
        private final DatumWriter<GenericRecord> datumWriter;
        private final CountingOutputStream countingOutputStream;
        private final BufferedOutputStream bufferedOutputStream;
        private Compressor mCompressor = null;


        public AvroDataFileWriter(LogFilePath path, CompressionCodec codec) throws IOException, ConfigurationException {
            Path fsPath = new Path(path.getLogFilePath());
            FileSystem fs = FileUtil.getFileSystem(path.getLogFilePath());
            File schemaFile = new File(SecorConfig.load().getMessageInputAvroSchema());
            Schema schema = new Schema.Parser().parse(schemaFile);
            this.countingOutputStream = new CountingOutputStream(fs.create(fsPath));
            this.datumWriter = new GenericDatumWriter<GenericRecord>(schema);
            this.innerWriter = new DataFileWriter<GenericRecord>(datumWriter);
            this.bufferedOutputStream = (codec == null) ? new BufferedOutputStream(this.countingOutputStream)
                    : new BufferedOutputStream(codec.createOutputStream(countingOutputStream,
                    mCompressor = CodecPool.getCompressor(codec)));
            this.innerWriter.create(schema, bufferedOutputStream);
        }

        @Override
        public long getLength() throws IOException {
            return countingOutputStream.getCount();
        }

        @Override
        public void write(KeyValue keyValue) throws IOException {
            innerWriter.appendEncoded(ByteBuffer.wrap(keyValue.getValue()));
        }

        @Override
        public void close() throws IOException {
            innerWriter.close();
            countingOutputStream.close();
            bufferedOutputStream.close();
        }
    }
}
