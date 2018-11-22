package com.gobblin.core;

/**
 * Created by Anirudha Sathe on 22/11/18.
 */

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.gobblin.configuration.ConfigurationKeys;
import org.apache.gobblin.configuration.WorkUnitState;
import org.apache.gobblin.converter.Converter;
import org.apache.gobblin.converter.DataConversionException;
import org.apache.gobblin.converter.SchemaConversionException;
import org.apache.gobblin.converter.SingleRecordIterable;
import org.apache.gobblin.converter.ToAvroConverterBase;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import com.google.common.base.Splitter;
import org.apache.gobblin.converter.avro.JsonElementConversionWithAvroSchemaFactory;

/**
 * {@link Converter} that takes an Avro schema from config and corresponding {@link JsonObject} records and
 * converts them to {@link GenericRecord} using the schema
 */

public class ComplexJsonConverter<SI> extends ToAvroConverterBase<SI, JsonObject> {

    private static final Splitter SPLITTER_ON_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    private Schema schema;
    private List<String> ignoreFields;

    public ToAvroConverterBase<SI, JsonObject> init(WorkUnitState workUnit) {
        super.init(workUnit);
        this.ignoreFields = SPLITTER_ON_COMMA.splitToList(workUnit.getProp(ConfigurationKeys.CONVERTER_IGNORE_FIELDS, ""));
        return this;
    }

    /**
     * Ignore input schema and parse in Avro schema from config
     */
    @Override
    public Schema convertSchema(SI inputSchema, WorkUnitState workUnit) throws SchemaConversionException {
        Preconditions.checkArgument(workUnit.contains(ConfigurationKeys.CONVERTER_AVRO_SCHEMA_KEY));
        this.schema = new Schema.Parser().parse(workUnit.getProp(ConfigurationKeys.CONVERTER_AVRO_SCHEMA_KEY));
        return this.schema;
    }

    /**
     * Take in {@link JsonObject} input records and convert them to {@link GenericRecord} using outputSchema
     */
    @Override
    public Iterable<GenericRecord> convertRecord(Schema outputSchema, JsonObject inputRecord, WorkUnitState workUnit)
            throws DataConversionException {
        GenericRecord avroRecord = convertNestedRecord(outputSchema, inputRecord, workUnit, this.ignoreFields);
        return new SingleRecordIterable<GenericRecord>(avroRecord);
    }

    public static GenericRecord convertNestedRecord(Schema outputSchema, JsonObject inputRecord, WorkUnitState workUnit,
                                                    List<String> ignoreFields) throws DataConversionException {
        GenericRecord avroRecord = new GenericData.Record(outputSchema);

        for (Schema.Field field : outputSchema.getFields()) {

            if (ignoreFields.contains(field.name())) {
                continue;
            }

            Schema.Type type = field.schema().getType();
            boolean nullable = false;
            Schema schema = field.schema();

            if (inputRecord.get(field.name()) == null) {
                throw new DataConversionException("Field missing from record: " + field.name());
            }

            if (type.equals(Schema.Type.RECORD)) {
                if (nullable || inputRecord.get(field.name()).isJsonNull()) {
                    avroRecord.put(field.name(), null);
                } else {
                    avroRecord.put(field.name(),
                            convertNestedRecord(schema, inputRecord.get(field.name()).getAsJsonObject(), workUnit, ignoreFields));
                }
            } else {
                avroRecord.put(field.name(), inputRecord.get(field.name()));
            }
        }
        return avroRecord;
    }
}