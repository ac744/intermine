package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for RGD genes.
 *
 * @author Fengyuan Hu
 */
public class RgdIdentifiersResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(RgdIdentifiersResolverFactory.class);

    // data file path set in ~/.intermine/MINE.properties
    // e.g. resolver.zfin.file=/micklem/data/rgd-identifiers/current/GENES_RAT.txt
    private final String propName = "resolver.rgd.file";
    private final String taxonId = "10116";

    /**
     * Construct with SO term of the feature type.
     */
    public RgdIdentifiersResolverFactory() {
        this.clsCol = this.defaultClsCol;
    }

    /**
     * Construct with SO term of the feature type.
     * TODO as class name is fixed as gene, this method is not useful
     * @param soTerm the feature type to resolve
     */
    public RgdIdentifiersResolverFactory(String clsName) {
        this.clsCol = new HashSet<String>(Arrays.asList(new String[] {clsName}));
    }

    @Override
    protected void createIdResolver() {
        if (resolver != null
                && resolver.hasTaxonAndClassName(taxonId, this.clsCol
                        .iterator().next())) {
            return;
        } else {
            if (resolver == null) {
                if (clsCol.size() > 1) {
                    resolver = new IdResolver();
                } else {
                    resolver = new IdResolver(clsCol.iterator().next());
                }
            }
        }

        try {
            boolean isCachedIdResolverRestored = restoreFromFile(this.clsCol);
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonAndClassName(taxonId, this.clsCol.iterator().next()))) {
                Properties props = PropertiesUtil.getProperties();
                String fileName = props.getProperty(propName);

                if (StringUtils.isBlank(fileName)) {
                    String message = "RGD gene resolver has no file name specified, set "
                            + propName + " to the location of the gene_info file.";
                    LOG.warn(message);
                    return;
                }

                LOG.info("Creating id resolver from data file and caching it.");
                createFromFile(new BufferedReader(new FileReader(new File(fileName))));
                resolver.writeToFile(new File(ID_RESOLVER_CACHED_FILE_NAME));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createFromFile(BufferedReader reader) throws IOException {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line[0].startsWith("GENE_RGD_ID") || line[0].startsWith("#")) {
                continue;
            }

            String rgdId = "RGD:" + line[0];
            String symbol = line[1];
            String name = line[2];
            String entrez = line[20];
            String ensembl = line[37];

            resolver.addMainIds(taxonId, rgdId, Collections.singleton(rgdId));
            resolver.addSynonyms(taxonId, rgdId, Collections.singleton(symbol));

            Set<String> ensemblIds = parseEnsemblIds(ensembl);
            resolver.addSynonyms(taxonId, rgdId, ensemblIds);

            if (!StringUtils.isBlank(name)) {
                resolver.addSynonyms(taxonId, rgdId, Collections.singleton(name));
            }

            if (!StringUtils.isBlank(entrez)) {
                resolver.addSynonyms(taxonId, rgdId, Collections.singleton(entrez));
            }
        }
    }

    private Set<String> parseEnsemblIds(String fromFile) {
        Set<String> ensembls = new HashSet<String>();
        if (!StringUtils.isBlank(fromFile)) {
            ensembls.addAll(Arrays.asList(fromFile.split(";")));
        }
        return ensembls;
    }
}
