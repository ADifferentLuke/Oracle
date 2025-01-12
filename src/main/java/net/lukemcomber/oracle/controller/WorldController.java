package net.lukemcomber.oracle.controller;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.genetics.Ecosystem;
import net.lukemcomber.genetics.MultiEpochEcosystem;
import net.lukemcomber.genetics.SteppableEcosystem;
import net.lukemcomber.genetics.biology.Cell;
import net.lukemcomber.genetics.biology.Gene;
import net.lukemcomber.genetics.biology.Genome;
import net.lukemcomber.genetics.biology.Organism;
import net.lukemcomber.genetics.biology.plant.cells.LeafCell;
import net.lukemcomber.genetics.biology.plant.cells.RootCell;
import net.lukemcomber.genetics.biology.plant.cells.SeedCell;
import net.lukemcomber.genetics.biology.plant.cells.StemCell;
import net.lukemcomber.genetics.io.CellHelper;
import net.lukemcomber.genetics.io.GenomeSerDe;
import net.lukemcomber.genetics.io.SpatialNormalizer;
import net.lukemcomber.genetics.model.SpatialCoordinates;
import net.lukemcomber.genetics.model.UniverseConstants;
import net.lukemcomber.genetics.model.ecosystem.EcosystemDetails;
import net.lukemcomber.genetics.model.ecosystem.impl.MultiEpochConfiguration;
import net.lukemcomber.genetics.model.ecosystem.impl.SteppableEcosystemConfiguration;
import net.lukemcomber.genetics.store.MetadataStore;
import net.lukemcomber.genetics.store.MetadataStoreFactory;
import net.lukemcomber.genetics.store.MetadataStoreGroup;
import net.lukemcomber.genetics.store.SearchableMetadataStore;
import net.lukemcomber.genetics.store.metadata.Environment;
import net.lukemcomber.genetics.store.metadata.Performance;
import net.lukemcomber.genetics.universes.FlatFloraUniverse;
import net.lukemcomber.genetics.world.terrain.Terrain;
import net.lukemcomber.genetics.world.terrain.TerrainProperty;
import net.lukemcomber.oracle.model.*;
import net.lukemcomber.oracle.model.net.*;
import net.lukemcomber.oracle.service.WorldCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("genetics")
public class WorldController {

    private Logger logger = LoggerFactory.getLogger(WorldController.class);

    public enum AvailableMetadata {
        ENVIRONMENT,
        PERFORMANCE
    }

    @Autowired
    private WorldCache cache;


    @PostMapping("v1.0/world")
    public ResponseEntity<CreateWorldResponse> createWorld(@RequestBody final CreateWorldRequest request) {

        final CreateWorldResponse response = new CreateWorldResponse();

        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final UniverseConstants properties = new FlatFloraUniverse();
        Ecosystem newEcosystem;

        try {

            final SpatialCoordinates dimensionsSpace = new SpatialCoordinates(request.width, request.height, request.depth);
            final SpatialNormalizer normalizer = new SpatialNormalizer();
            final Map<SpatialCoordinates, String> startingOrganisms;

            if (Objects.nonNull(request.zoology)) {
                startingOrganisms = normalizer.normalize(dimensionsSpace, request.zoology);
            } else {
                startingOrganisms = null;
            }

            final Callable<Void> initializationFunction;
            if (request instanceof CreateSteppableWorld steppableConfiguration) {

                newEcosystem = new SteppableEcosystem(properties,
                        SteppableEcosystemConfiguration.builder()
                                .ticksPerTurn(steppableConfiguration.ticksPerTurn)
                                .ticksPerDay(steppableConfiguration.ticksPerDay)
                                .size(dimensionsSpace)
                                .name(steppableConfiguration.name)
                                .startOrganisms(startingOrganisms)
                                .build());
                initializationFunction = () -> {
//                    cache.remove(newEcosystem.getId());
                    return null;
                };

                response.id = newEcosystem.getId();
                cache.set(newEcosystem);

            } else if (request instanceof CreateAutoWorldRequest epochConfiguration) {

                final MultiEpochEcosystem multiEpochEcosystem= new MultiEpochEcosystem(properties, MultiEpochConfiguration.builder()
                        .ticksPerDay(epochConfiguration.ticksPerDay)
                        .size(dimensionsSpace)
                        .maxDays(epochConfiguration.maxDays)
                        .tickDelayMs(epochConfiguration.tickDelay)
                        .name(epochConfiguration.name)
                        .deleteFilterOnExit(false)
                        .fileFilterPath(epochConfiguration.filterPath)
                        .epochs(epochConfiguration.epochs)
                        .reusePopulation(epochConfiguration.reusePopulation)
                        .initialPopulation(epochConfiguration.initialPopulation)
                        .startOrganisms(startingOrganisms)
                        .build(),
                        ecosystem -> {
                            cache.set(ecosystem);
                        }, null);
                newEcosystem = multiEpochEcosystem;
                initializationFunction = () -> {
                    final ConcurrentMap<String, Ecosystem> sessions = multiEpochEcosystem.getEpochs();
                    for( final String ids : sessions.keySet()){
                        cache.remove(ids);
                    }
                    return null;
                };
            } else {
                throw new RuntimeException();
            }

            newEcosystem.initialize(initializationFunction);

            response.setStatusCode(HttpStatus.OK);

        } catch (final IOException e) {
            response.setMessage(e.getMessage());
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/list")
    public ResponseEntity<WorldsListResponse> getWorldsList() {
        /*
         * DEV NOTE: if we ever add multiple user sessions or authentication, this
         *    should be put behind admin access
         */
        final WorldsListResponse response = new WorldsListResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final List<WorldsListResponse.WorldOverview> overviews = new LinkedList<>();
        for (final Ecosystem system : cache.values()) {
            if (null != system) {
                final WorldsListResponse.WorldOverview overview = new WorldsListResponse.WorldOverview();
                overview.active = system.isActive();
                overview.id = system.getId();
                overview.steppable = system instanceof SteppableEcosystem;
                overview.name = system.getName();
                if (Objects.nonNull(system.getTerrain())) {
                    final Terrain terrain = system.getTerrain();
                    overview.totalOrganisms = terrain.getTotalOrganismCount();
                    overview.currentOrganisms = terrain.getOrganismCount();
                }
                overviews.add(overview);
            }
        }
        response.worlds = overviews;
        response.setStatusCode(HttpStatus.OK);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}")
    public ResponseEntity<GenericResponse<EcosystemDetails>> getWorldSummary(@PathVariable final String id,
                                                                             @RequestParam(value = "verbose", defaultValue = "false") final boolean verbose) {
        final GenericResponse<EcosystemDetails> retVal = new GenericResponse<>();

        retVal.setStatusCode(HttpStatus.BAD_REQUEST);

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                retVal.result = system.getSetupConfiguration();
                retVal.setStatusCode(HttpStatus.OK);

            } else {
                retVal.setStatusCode(HttpStatus.BAD_REQUEST);
                retVal.setMessage("Ecosystem " + id + " not found.");
            }
        }
        return ResponseEntity.status(retVal.getStatusCode()).body(retVal);

    }

    @GetMapping("v1.0/{id}/advance")
    public ResponseEntity<AdvanceWorldResponse> advanceWorld(@PathVariable(name = "id") final String id,
                                                             @RequestParam(name = "turns", required = false, defaultValue = "1") final Integer turns) {

        final AdvanceWorldResponse response = new AdvanceWorldResponse();

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (system instanceof SteppableEcosystem steppableEcosystem) {
                response.active = steppableEcosystem.isActive();
                if (steppableEcosystem.isActive()) {
                    int i = 0;
                    for (; i < turns; ++i) {
                        if (!steppableEcosystem.advance()) {
                            response.active = false;
                            break;
                        }
                    }
                    response.ticksMade = (long) i;
                    response.setStatusCode(HttpStatus.OK);
                } else {
                    response.setMessage(String.format("Simulation %s is not interactive.", id));
                }
            } else {
                response.setMessage(String.format("Session $s not found.", id));
            }
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping(path = "v1.0/{id}/ecology/image", produces = "image/jpeg")
    @Cacheable(value = "nocache", condition = "#result == null")
    public BufferedImage getEcologyImage(@PathVariable(name = "id") String id,
                                         @RequestParam(name = "width", required = false) final Integer imageWidth,
                                         @RequestParam(name = "height", required = false) final Integer imageHeight) {
        final long startTime = -System.currentTimeMillis();
        BufferedImage imageOut = null;

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {

                final int witdh = system.getTerrain().getSizeOfXAxis();
                final int height = system.getTerrain().getSizeOfYAxis();

                final int outputWidth = null != imageWidth ? imageWidth : witdh;
                final int outputHeight = null != imageHeight ? imageHeight : height;

                int cellWidth = outputWidth / witdh;
                int cellHeight = outputHeight / height;

                imageOut = new BufferedImage(outputWidth, outputHeight,
                        BufferedImage.TYPE_3BYTE_BGR);
                final Graphics graphics = imageOut.getGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, outputWidth, outputHeight);

                final Iterator<Organism> iterator = system.getTerrain().getOrganisms();
                while (iterator.hasNext()) {
                    final Organism organism = iterator.next();
                    for (final Cell cell : CellHelper.getAllOrganismsCells(organism.getFirstCell())) {
                        final SpatialCoordinates spatialCoordinates = cell.getCoordinates();
                        int red = 0;
                        int blue = 0;
                        int green = 0;

                        if (organism.isAlive()) {

                            switch (cell.getCellType()) {
                                case SeedCell.TYPE -> {
                                    red = 112;
                                    green = 82;
                                    blue = 252;
                                }
                                case LeafCell.TYPE -> {
                                    red = 46;
                                    green = 130;
                                    blue = 48;
                                }
                                case RootCell.TYPE -> {
                                    red = 139;
                                    green = 69;
                                    blue = 19;
                                }
                                case StemCell.TYPE -> {
                                    red = 82;
                                    green = 252;
                                    blue = 87;
                                }
                            }

                        } else {
                            red = 129;
                            green = 129;
                            blue = 129;
                        }

                        int rgb = (red & 0xFF) << 16
                                | (green & 0xFF) << 8
                                | (blue & 0xFF);

                        int rectX = spatialCoordinates.xAxis() * cellWidth;
                        int rectY = spatialCoordinates.yAxis() * cellHeight;
                        int rectW = cellWidth;
                        int rectH = cellHeight;

                        logger.debug(String.format("Writing %s  as (%d,%d,%d,%d)", spatialCoordinates,
                                rectX, rectY, rectW, rectH));

                        for (int i = rectX; i < rectX + rectW; i++) {
                            for (int j = rectY; j < rectY + rectH; j++) {
                                imageOut.setRGB(i, j, rgb);
                            }
                        }
                    }
                }
                imageOut.flush();
            }
        }
        //logger.error("Time " + (System.currentTimeMillis() + startTime) + " Organisms " + organismDebug + " cell count " + cellCount);
        return imageOut;
    }

    @GetMapping("v1.0/{id}/ecology")
    public ResponseEntity<EcologyResponse> getEcology(@PathVariable(name = "id") String id) {
        final EcologyResponse response = new EcologyResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        if (StringUtils.isNotEmpty(id)) {

            final Ecosystem system = cache.get(id);
            if (null != system) {

                response.currentTick = system.getCurrentTick();
                response.totalDays = system.getTotalDays();
                response.totalTicks = system.getTotalTicks();
                response.active = system.isActive();

                if (system.isActive()) {
                    //get list of cells
                    final Iterator<Organism> iterator = system.getTerrain().getOrganisms();
                    while (iterator.hasNext()) {
                        final Organism organism = iterator.next();
                        final OrganismBody body = new OrganismBody();
                        body.setId(organism.getUniqueID());
                        body.setAlive(organism.isAlive());
                        for (final Cell cell : CellHelper.getAllOrganismsCells(organism.getFirstCell())) {
                            final SpatialCoordinates spatialCoordinates = cell.getCoordinates();
                            final CellLocation location;
                            if (cell instanceof SeedCell) {
                                location = new SeedCellLocation(spatialCoordinates.xAxis(), spatialCoordinates.yAxis(),
                                        spatialCoordinates.zAxis(), cell.getCellType(), ((SeedCell) cell).isActivated());
                            } else {
                                location = new CellLocation(spatialCoordinates.xAxis(), spatialCoordinates.yAxis(),
                                        spatialCoordinates.zAxis(), cell.getCellType());
                            }
                            body.addCell(location);
                        }
                        response.organismBodies.add(body);
                    }
                }
                response.setStatusCode(HttpStatus.OK);
            }
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}/inspect")
    public ResponseEntity<InspectResponse> getLocationInformation(@PathVariable(name = "id") final String worldId,
                                                                  @RequestParam(name = "x") final int xCoord,
                                                                  @RequestParam(name = "y") final int yCoord,
                                                                  @RequestParam(name = "z") final int zCoord) {
        final InspectResponse response = new InspectResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final Ecosystem ecosystem = cache.get(worldId);
        if (null != ecosystem) {
            if (0 <= xCoord && 0 <= yCoord && 0 <= zCoord) {
                //We don't do negative space
                final SpatialCoordinates location = new SpatialCoordinates(xCoord, yCoord, zCoord);
                final List<TerrainProperty> properties = ecosystem.getTerrain().getTerrainProperties(location);

                response.setCoordinates(location);
                for (final TerrainProperty terrainProperty : properties) {
                    response.addEnvironmentValue(terrainProperty.getId(), terrainProperty.getValue().toString());
                }
                final Cell cell = ecosystem.getTerrain().getCell(location);
                final Organism organism = ecosystem.getTerrain().getOrganism(location);
                if (null != cell) {
                    response.addOccupantValue("cell", cell.getCellType());
                    response.addOccupantValue("organism", null != organism ? organism.getUniqueID() : " null ");
                    response.addOccupantValue("cell cost", String.valueOf(cell.getMetabolismCost()));

                    /* Need a better place for this */
                    response.addOccupantValue("organism energy", String.valueOf(organism.getEnergy()));
                    response.addOccupantValue("organism cost", String.valueOf(organism.getMetabolismCost()));
                    response.addOccupantValue("organism birth", String.valueOf(organism.getBirthTick()));
                    response.addOccupantValue("organism updated", String.valueOf(organism.getLastUpdatedTick()));
                }
                response.setStatusCode(HttpStatus.OK);
            } else {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                response.setMessage(String.format("Invalid coordinates (%d,%d)", xCoord, yCoord));
            }
        } else {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            response.setMessage("World not found: " + worldId);
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}/inspect/{oid}")
    public ResponseEntity<InspectOrganismResponse> getOrganismsGenes(
            @PathVariable(name = "id") final String worldId,
            @PathVariable(name = "oid") final String organismId) {

        final InspectOrganismResponse response = new InspectOrganismResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        if (StringUtils.isNotEmpty(worldId) && StringUtils.isNotEmpty(organismId)) {
            final Ecosystem ecosystem = cache.get(worldId);
            if (null != ecosystem) {

                final Organism organism = ecosystem.getTerrain().getOrganism(organismId);
                if (null != organism) {
                    //we found our baby
                    response.organism = new OrganismDetails();
                    response.organism.age = null;
                    response.organism.energy = organism.getEnergy();
                    response.organism.genes = new LinkedList<>();
                    response.organism.name = organismId;
                    response.organism.genus = organism.getOrganismType();
                    response.organism.parentId = organism.getParentId();
                    final Genome genome = organism.getGenome();
                    response.organism.genome = GenomeSerDe.serialize(genome);

                    for (int i = 0; genome.getNumberOfGenes() > i; ++i) {
                        final Gene gene = genome.getGeneNumber(i);
                        if (null != gene) {
                            response.organism.genes.add(gene);
                        }
                    }
                    response.setStatusCode(HttpStatus.OK);
                }
            } else {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                response.setMessage("World " + worldId + " not found.");
            }
        } else {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            response.setMessage("World or organism id is null!");
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}/log/{type}")
    public ResponseEntity<LogResponse> getMetadata(@PathVariable(name = "id") final String worldId,
                                                   @PathVariable(name = "type") final AvailableMetadata type,
                                                   @RequestParam(name = "metric", required = false) final String metric,
                                                   @RequestParam(name = "page", required = false, defaultValue = "0") final int page,
                                                   @RequestParam(name = "count", required = false, defaultValue = "50") final int count
    ) {

        final LogResponse response = new LogResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final ObjectMapper mapper = new ObjectMapper();
        if (StringUtils.isNotEmpty(worldId)) {
            final Ecosystem ecosystem = cache.get(worldId);
            if (null != ecosystem) {
                final Class clazz;
                switch (type) {
                    case PERFORMANCE -> {
                        clazz = Performance.class;
                    }
                    case ENVIRONMENT -> {
                        clazz = Environment.class;
                    }
                    default -> {
                        clazz = null;
                    }

                }
                try {
                    if (null != clazz) {
                        final MetadataStoreGroup sessionGroup =
                                MetadataStoreFactory.getMetadataStore(worldId, ecosystem.getTerrain().getProperties());

                        final MetadataStore<?> store = sessionGroup.get(clazz);
                        final int recordCount = 0 == page && 0 > count ? (int) store.count() : count;
                        if (!store.isExpired()) {
                            final List<?> pageList;
                            if (StringUtils.isNotEmpty(metric) && store instanceof SearchableMetadataStore<?>) {
                                pageList = ((SearchableMetadataStore<?>) store).page(metric, page, recordCount);
                            } else {
                                pageList = store.page(null, page, recordCount);
                            }
                            response.log = mapper.valueToTree(pageList);
                            response.setStatusCode(HttpStatus.OK);
                        } else {
                            response.setMessage("This information has expired or was not collected.");
                        }
                    } else {
                        response.setMessage(String.format("%s is not a supported metadata collection.", clazz.getSimpleName()));
                    }


                } catch (final IOException e) {
                    response.setMessage(e.getMessage());
                    logger.error("", e);
                }
            }
        }
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
