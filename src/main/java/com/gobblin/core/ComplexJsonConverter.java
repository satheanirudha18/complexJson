package com.gobblin.core;

/**
 * Created by Anirudha Sathe on 22/11/18.
 */

import java.util.List;

import com.google.gson.*;
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
import com.google.common.base.Splitter;
import org.apache.gobblin.converter.avro.JsonElementConversionWithAvroSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Converter} that takes an Avro schema from config and corresponding {@link JsonObject} records and
 * converts them to {@link GenericRecord} using the schema
 */

public class ComplexJsonConverter<SI> extends ToAvroConverterBase<SI, String> {

    private static final Splitter SPLITTER_ON_COMMA = Splitter.on(',').trimResults().omitEmptyStrings();
    private Schema schema;
    private List<String> ignoreFields;
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexJsonConverter.class);
    private static final Gson GSON = new Gson();

    public ToAvroConverterBase<SI, String> init(WorkUnitState workUnit) {
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
    public Iterable<GenericRecord> convertRecord(Schema outputSchema, String inputRecord, WorkUnitState workUnit)
            throws DataConversionException {
        JsonObject inputs = GSON.fromJson(inputRecord, JsonObject.class);
        GenericRecord avroRecord = convertNestedRecord(outputSchema, inputs, workUnit, this.ignoreFields);
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

            if (type.equals(Schema.Type.ARRAY)) {
                String arraySchema = schema.toString();
                int second_colon = arraySchema.indexOf(":", arraySchema.indexOf(":") + 1);
                arraySchema = arraySchema.substring(second_colon+1, arraySchema.length()-1);
                Schema new_schema = new Schema.Parser().parse(arraySchema);

                JsonObject jsonObject = inputRecord.get(field.name()).getAsJsonObject();

                LOGGER.info("Nested field --> " + jsonObject.entrySet().toString());
                String partitionArray = "[employee]";

                String arrayColumn = partitionArray.substring(1, partitionArray.length()-1);
                JsonArray jsonArray = jsonObject.getAsJsonArray(arrayColumn);

                for (JsonElement jsonElement : jsonArray) {
                    JsonObject insideRecord = jsonElement.getAsJsonObject();
                    avroRecord.put(field.name(), convertNestedRecord(new_schema, insideRecord, workUnit, ignoreFields));
                }
            }

            if (type.equals(Schema.Type.RECORD)) {
                LOGGER.info("Parsing " + field.name() + " field......");
                if (nullable || inputRecord.get(field.name()).isJsonNull()) {
                    avroRecord.put(field.name(), null);
                } else {
                    avroRecord.put(field.name(),
                            convertNestedRecord(schema, inputRecord.get(field.name()).getAsJsonObject(), workUnit, ignoreFields));
                }
            } else {
                LOGGER.info("Field --> " + field.name() + ", Value Type--> " + inputRecord.get(field.name()).getClass().getName());
                String value = inputRecord.get(field.name()).getAsString();
                avroRecord.put(field.name(), value);
                LOGGER.info("Field --> " + field.name() + ", New Data Type --> " + value.getClass().getName());
            }
        }
        LOGGER.info("Avro Records Data Type --> " + avroRecord.getClass().getName());
        return avroRecord;
    }
}
