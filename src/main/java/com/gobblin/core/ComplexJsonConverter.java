package com.gobblin.core;

/**
 * Created by Anirudha Sathe on 22/11/18.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Collection;

import com.google.gson.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.gobblin.configuration.ConfigurationKeys;
import org.apache.gobblin.configuration.WorkUnitState;
import org.apache.gobblin.converter.Converter;
import org.apache.gobblin.converter.DataConversionException;
import org.apache.gobblin.converter.SchemaConversionException;
import org.apache.gobblin.converter.SingleRecordIterable;
import org.apache.gobblin.converter.ToAvroConverterBase;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import org.json.XML;

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
    public static final String EVENT_SOURCE_TYPE = "event.source.type";

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
        LOGGER.info("Converting input records into GenericRecord according to the schema provided.");
        if (readProp(EVENT_SOURCE_TYPE, workUnit).equalsIgnoreCase("xml")){
            if (inputRecord.contains("&")){
                inputRecord = inputRecord.replace("&", "&amp;");
            }
            try{
                inputRecord = XML.toJSONObject(inputRecord).toString();
            }catch (JSONException je) {
                throw new DataConversionException("Exception occurred while converting XML to JSON --> " + je);
            }
            return new SingleRecordIterable<GenericRecord>(new JsonAvroConverter().convertToGenericDataRecord(XML.toJSONObject(inputRecord).toString().getBytes(),outputSchema));
        }else{
            return new SingleRecordIterable<GenericRecord>(new JsonAvroConverter().convertToGenericDataRecord(inputRecord.getBytes(),outputSchema));
        }
    }

    private static String readProp(String key, WorkUnitState workUnitState) {
        LOGGER.info("Key --> " + key);
        String value = workUnitState.getWorkunit().getProp(key);
        if (StringUtils.isBlank(value)) {
            value = workUnitState.getProp(key);
        }
        if (StringUtils.isBlank(value)) {
            value = workUnitState.getJobState().getProp(key);
        }
        LOGGER.info(key + " --> " + value);
        return value;
    }
}