package com.gobblin.core;

/**
 * Created by Anirudha Sathe on 22/11/18.
 */

import java.io.IOException;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.apache.gobblin.configuration.ConfigurationKeys;
import org.apache.gobblin.configuration.SourceState;
import org.apache.gobblin.configuration.WorkUnitState;
import org.apache.gobblin.source.Source;
import org.apache.gobblin.source.extractor.Extractor;
import org.apache.gobblin.source.workunit.Extract;
import org.apache.gobblin.source.workunit.WorkUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of {@link Source} for the simple JSON example.
 *
 * <p>
 *   This source creates one {@link org.apache.gobblin.source.workunit.WorkUnit}
 *   for each file to pull and uses the {@link ComplexJsonExtractor} to pull the data.
 * </p>
 *
 * @author Yinan Li
 */
public class ComplexJsonSource implements Source<String, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexJsonExtractor.class);

    public static final String SOURCE_FILE_KEY = "source.file";

    public List<WorkUnit> getWorkunits(SourceState state) {
        List<WorkUnit> workUnits = Lists.newArrayList();

        if (!state.contains(ConfigurationKeys.SOURCE_FILEBASED_FILES_TO_PULL)) {
            return workUnits;
        }

        LOGGER.info("Found Extract Namespace Key - " + state.getProp(ConfigurationKeys.EXTRACT_NAMESPACE_NAME_KEY));

        LOGGER.info("Found Extract Table Name Key - " + state.getProp(ConfigurationKeys.EXTRACT_TABLE_NAME_KEY));

        // Create a single snapshot-type extract for all files
        Extract extract = new Extract(Extract.TableType.SNAPSHOT_ONLY,
                state.getProp(ConfigurationKeys.EXTRACT_NAMESPACE_NAME_KEY, "ComplexJson"), ConfigurationKeys.EXTRACT_TABLE_NAME_KEY);

        String filesToPull = state.getProp(ConfigurationKeys.SOURCE_FILEBASED_FILES_TO_PULL);
        for (String file : Splitter.on(',').omitEmptyStrings().split(filesToPull)) {
            // Create one work unit for each file to pull
            WorkUnit workUnit = WorkUnit.create(extract);
            workUnit.setProp(SOURCE_FILE_KEY, file);
            workUnits.add(workUnit);
        }

        return workUnits;
    }

    public Extractor<String, String> getExtractor(WorkUnitState state) throws IOException {
        return new ComplexJsonExtractor(state);
    }

    public void shutdown(SourceState state) {
        // Nothing to do
    }
}