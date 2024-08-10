package net.lukemcomber.oracle.controller;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.genetics.AutomaticEcosystem;
import net.lukemcomber.genetics.SteppableEcosystem;
import net.lukemcomber.genetics.biology.Cell;
import net.lukemcomber.genetics.biology.Gene;
import net.lukemcomber.genetics.biology.Genome;
import net.lukemcomber.genetics.biology.Organism;
import net.lukemcomber.genetics.biology.plant.cells.LeafCell;
import net.lukemcomber.genetics.biology.plant.cells.RootCell;
import net.lukemcomber.genetics.biology.plant.cells.SeedCell;
import net.lukemcomber.genetics.biology.plant.cells.StemCell;
import net.lukemcomber.genetics.exception.EvolutionException;
import net.lukemcomber.genetics.model.SpatialCoordinates;
import net.lukemcomber.genetics.model.ecosystem.EcosystemConfiguration;
import net.lukemcomber.genetics.service.CellHelper;
import net.lukemcomber.genetics.service.EcoSystemJsonReader;
import net.lukemcomber.genetics.service.GenomeSerDe;
import net.lukemcomber.genetics.Ecosystem;
import net.lukemcomber.genetics.store.MetadataStore;
import net.lukemcomber.genetics.store.MetadataStoreFactory;
import net.lukemcomber.genetics.store.MetadataStoreGroup;
import net.lukemcomber.genetics.store.SearchableMetadataStore;
import net.lukemcomber.genetics.store.metadata.Environment;
import net.lukemcomber.genetics.store.metadata.Performance;
import net.lukemcomber.genetics.utilities.SimpleSimulator;
import net.lukemcomber.genetics.utilities.model.SimpleSimulation;
import net.lukemcomber.genetics.utilities.model.SimulationSessions;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("genetics")
public class WorldController {

    public static final int WAIT_TIME_FOR_AUTO_WORLD_MS = 5;
    public static final int RETRY_COUNT = 5;

    private Logger logger = LoggerFactory.getLogger(WorldController.class);

    private final static AtomicBoolean runningSimulation = new AtomicBoolean(false);

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

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.valueToTree(request);
        final int epochs = rootNode.path("epochs").asInt(0);

        try {
            if (0 >= epochs) {
                final Ecosystem system = new EcoSystemJsonReader().read(rootNode);
                if (null != system) {
                    response.id = cache.set(system);
                    response.setStatusCode(HttpStatus.OK);
                } else {
                    response.setMessage("No terrain created.");
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

                }
            } else if (request instanceof CreateAutoWorldRequest) {
                final SimpleSimulation simulationParameters = new SimpleSimulation();
                final CreateAutoWorldRequest autoRequest = (CreateAutoWorldRequest) request;

                simulationParameters.epochs = epochs;
                simulationParameters.height = autoRequest.height;
                simulationParameters.width = autoRequest.width;
                simulationParameters.maxDays = autoRequest.maxDays;
                simulationParameters.ticksPerDay = autoRequest.ticksPerDay;
                simulationParameters.initialPopulation = autoRequest.initialPopulation;
                simulationParameters.reusePopulation = autoRequest.reusePopulation;
                simulationParameters.name = autoRequest.name;
                if (null != autoRequest.zoology) {
                    simulationParameters.startOrganisms = new HashMap<>();
                    for (int i = 0; i < autoRequest.zoology.size(); i++) {
                        final String strInputOrganism = autoRequest.zoology.get(i);
                        if (StringUtils.isNotEmpty(strInputOrganism)) {
                            final int splitIndex = strInputOrganism.lastIndexOf(',');
                            if (0 < splitIndex) {
                                final String coordinates = strInputOrganism.substring(0, splitIndex);
                                final String genome = strInputOrganism.substring(splitIndex + 1);

                                final SpatialCoordinates spatialCoordinates = SpatialCoordinates.fromString(coordinates);
                                simulationParameters.startOrganisms.put(spatialCoordinates, genome);

                            } else {
                                throw new EvolutionException("Invalid format: : " + strInputOrganism);
                            }
                        }
                    }
                }

                response.id = startAutoSimulation(simulationParameters);
                if (StringUtils.isEmpty(response.id)) {
                    throw new EvolutionException("Could not determine world id. Either an error occurred or system is overloaded.");
                }
                response.setStatusCode(HttpStatus.OK);
            }
        } catch (final RuntimeException | IOException | InterruptedException e) {
            logger.error("", e);
            response.setMessage(e.getMessage());
        }

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    private String startAutoSimulation(final SimpleSimulation simulation) throws IOException, InterruptedException {
        String createdWorldId = null;
        int waitTime = WAIT_TIME_FOR_AUTO_WORLD_MS;
        if (!runningSimulation.get()) {
            synchronized (runningSimulation) {
                if (!runningSimulation.get()) {

                    runningSimulation.set(true);
                    final File tmpFilePath = Files.createTempFile("store-", "filter").toFile();
                    final SimpleSimulator simulator = new SimpleSimulator(simulation, tmpFilePath);
                    final SimulationSessions sessions = simulator.getSessions();
                    cache.addSimulationSession(sessions);

                    final Runnable runThread = () -> {
                        try {
                            simulator.run(simulation.startOrganisms);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            synchronized (runningSimulation) {
                                runningSimulation.set(false);
                            }
                        }
                    };
                    Thread thread = new Thread(runThread);
                    thread.setDaemon(true);
                    thread.setName("auto-simulation");
                    thread.start();

                    int retries = 0;
                    while (null == createdWorldId && RETRY_COUNT > retries++) {
                        if (0 < sessions.ids().size()) {
                            createdWorldId = sessions.ids().stream().findFirst().orElse(null);
                        } else {
                            logger.info("Sleeping for world id for " + waitTime);
                            Thread.sleep(waitTime);
                            waitTime = waitTime * (3 * waitTime / 3);
                        }
                    }

                }
            }
        } else {
            throw new EvolutionException("A multi-epoch simulation is already running.");
        }
        return createdWorldId;
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
                if( Objects.nonNull(system.getTerrain())){
                    final Terrain terrain = system.getTerrain();
                    overview.totalOrganisms = terrain.getTotalOrganismCount();
                    overview.currentOrganisms = terrain.getOrganismCount();
                }
                overviews.add(overview);
            }
        }
        response.worlds = overviews;
        response.simulationRunning = runningSimulation.get();
        response.setStatusCode(HttpStatus.OK);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}")
    public ResponseEntity<GenericResponse<EcosystemConfiguration>> getWorldSummary(@PathVariable final String id,
                                                                                   @RequestParam(value = "verbose", defaultValue = "false") final boolean verbose) {
        final GenericResponse<EcosystemConfiguration> retVal = new GenericResponse<>();

        retVal.setStatusCode(HttpStatus.BAD_REQUEST);

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                final EcosystemConfiguration ecosystemConfiguration = system.getSetupConfiguration();
                retVal.result = ecosystemConfiguration;
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
            if (null != system) {
                if (system instanceof SteppableEcosystem) {
                    response.active = system.isActive();
                    if (system.isActive()) {
                        int i = 0;
                        for (; i < turns; ++i) {
                            if (!system.advance()) {
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
                    response.setMessage(String.format("Simulation unable to advance session %s.", id));
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
        int organismDebug = 0;
        long cellCount = 0;
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
                    organismDebug++;
                    for (final Cell cell : CellHelper.getAllOrganismsCells(organism.getCells())) {
                        final SpatialCoordinates spatialCoordinates = cell.getCoordinates();
                        cellCount++;
                        int red = 0;
                        int blue = 0;
                        int green = 0;
                        int alpha = 0;

                        if (organism.isAlive()) {

                            switch (cell.getCellType()) {
                                case SeedCell.TYPE -> {
                                    red = 112;
                                    green = 82;
                                    blue = 252;
                                    alpha = 255;
                                }
                                case LeafCell.TYPE -> {
                                    red = 46;
                                    green = 130;
                                    blue = 48;
                                    alpha = 255;
                                }
                                case RootCell.TYPE -> {
                                    red = 139;
                                    green = 69;
                                    blue = 19;
                                    alpha = 255;
                                }
                                case StemCell.TYPE -> {
                                    red = 82;
                                    green = 252;
                                    blue = 87;
                                    alpha = 255;
                                }
                            }

                        } else {
                            red = 129;
                            green = 129;
                            blue = 129;
                            alpha = 255;
                        }

                        int rgb = (red & 0xFF) << 16
                                | (green & 0xFF) << 8
                                | (blue & 0xFF);

                        int rectX = spatialCoordinates.xAxis * cellWidth;
                        int rectY = spatialCoordinates.yAxis * cellHeight;
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
                        for (final Cell cell : CellHelper.getAllOrganismsCells(organism.getCells())) {
                            final SpatialCoordinates spatialCoordinates = cell.getCoordinates();
                            final CellLocation location;
                            if (cell instanceof SeedCell) {
                                location = new SeedCellLocation(spatialCoordinates.xAxis, spatialCoordinates.yAxis,
                                        spatialCoordinates.zAxis, cell.getCellType(), ((SeedCell) cell).isActivated());
                            } else {
                                location = new CellLocation(spatialCoordinates.xAxis, spatialCoordinates.yAxis,
                                        spatialCoordinates.zAxis, cell.getCellType());
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
                final List<TerrainProperty> properties = ecosystem.getTerrain().getTerrain(location);

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
                        if (!store.expire()) {
                            final List<?> pageList;
                            if (StringUtils.isNotEmpty(metric) && store instanceof SearchableMetadataStore<?>) {
                                pageList = ((SearchableMetadataStore<?>) store).page(metric, page, recordCount);
                            } else {
                                pageList = store.page(page, recordCount);
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
