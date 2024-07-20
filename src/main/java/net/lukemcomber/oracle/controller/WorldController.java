package net.lukemcomber.oracle.controller;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.genetics.SteppableEcosystem;
import net.lukemcomber.genetics.biology.Cell;
import net.lukemcomber.genetics.biology.Gene;
import net.lukemcomber.genetics.biology.Genome;
import net.lukemcomber.genetics.biology.Organism;
import net.lukemcomber.genetics.biology.plant.cells.SeedCell;
import net.lukemcomber.genetics.model.SpatialCoordinates;
import net.lukemcomber.genetics.service.CellHelper;
import net.lukemcomber.genetics.service.EcoSystemJsonReader;
import net.lukemcomber.genetics.service.GenomeSerDe;
import net.lukemcomber.genetics.Ecosystem;
import net.lukemcomber.genetics.store.MetadataStore;
import net.lukemcomber.genetics.store.MetadataStoreFactory;
import net.lukemcomber.genetics.store.MetadataStoreGroup;
import net.lukemcomber.genetics.store.metadata.Genealogy;
import net.lukemcomber.genetics.store.metadata.Performance;
import net.lukemcomber.genetics.world.terrain.Terrain;
import net.lukemcomber.genetics.world.terrain.TerrainProperty;
import net.lukemcomber.oracle.model.*;
import net.lukemcomber.oracle.model.net.*;
import net.lukemcomber.oracle.service.WorldCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("genetics")
public class WorldController {

    private Logger logger = LoggerFactory.getLogger(WorldController.class);

    public enum AvailableMetadata {
        GENOME,
        GENEALOGY,
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

        try {
            final Ecosystem system = new EcoSystemJsonReader().read(rootNode);
            if (null != system) {
                response.id = cache.set(system);
                response.setStatusCode(HttpStatus.OK);
            } else {
                response.setMessage("No terrain created.");
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (final RuntimeException | IOException e) {
            logger.error("", e);
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
                overviews.add(overview);
            }
        }
        response.worlds = overviews;
        response.setStatusCode(HttpStatus.OK);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("v1.0/{id}")
    public ResponseEntity<WorldOverviewResponse> getWorldSummary(@PathVariable final String id) {
        final WorldOverviewResponse retVal = new WorldOverviewResponse();

        retVal.setStatusCode(HttpStatus.BAD_REQUEST);

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                final Terrain terrain = system.getTerrain();
                if (null != terrain) {
                    retVal.width = terrain.getSizeOfXAxis();
                    retVal.height = terrain.getSizeOfYAxis();
                    retVal.depth = terrain.getSizeOfZAxis();
                    retVal.active = system.isActive();
                    retVal.interactive = system instanceof SteppableEcosystem;
                    retVal.id = id;
                    retVal.setStatusCode(HttpStatus.OK);
                } else {
                    retVal.setStatusCode(HttpStatus.BAD_REQUEST);
                    retVal.setMessage("Terrain " + id + " found but was null.");
                }
            } else {
                retVal.setStatusCode(HttpStatus.BAD_REQUEST);
                retVal.setMessage("Terrain " + id + " not found.");
            }
        }
        return ResponseEntity.status(retVal.getStatusCode()).body(retVal);

    }

    //TODO may want to make this asynch so we don't tie up a request thread
    @GetMapping("v1.0/{id}/advance")
    public ResponseEntity<AdvanceWorldResponse> advanceWorld(@PathVariable(name = "id") final String id,
                                                             @RequestParam(name = "turns", required = false, defaultValue = "1") final Integer turns) {

        final AdvanceWorldResponse response = new AdvanceWorldResponse();

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                if (system instanceof SteppableEcosystem) {
                    response.active = system.isActive();
                    if (system.isActive()){
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
    public ResponseEntity<LogResponse> getOrganismHistory(@PathVariable(name = "id") final String worldId,
                                                          @PathVariable(name = "type") final AvailableMetadata type) {

        final LogResponse response = new LogResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final ObjectMapper mapper = new ObjectMapper();
        if (StringUtils.isNotEmpty(worldId)) {
            final Ecosystem ecosystem = cache.get(worldId);
            if (null != ecosystem) {
                final Class clazz;
                switch (type) {
                    case GENOME -> {
                        clazz = net.lukemcomber.genetics.store.metadata.Genome.class;
                    }
                    case GENEALOGY -> {
                        clazz = Genealogy.class;
                    }
                    case PERFORMANCE -> {
                        clazz = Performance.class;
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
                        if (!store.expire()) {
                            response.log = mapper.valueToTree(store.retrieve());
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
