package net.lukemcomber.oracle.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.dev.ai.genetics.biology.Cell;
import net.lukemcomber.dev.ai.genetics.biology.Gene;
import net.lukemcomber.dev.ai.genetics.biology.Genome;
import net.lukemcomber.dev.ai.genetics.biology.Organism;
import net.lukemcomber.dev.ai.genetics.biology.plant.cells.SeedCell;
import net.lukemcomber.dev.ai.genetics.model.SpatialCoordinates;
import net.lukemcomber.dev.ai.genetics.service.CellHelper;
import net.lukemcomber.dev.ai.genetics.service.EcoSystemJsonReader;
import net.lukemcomber.dev.ai.genetics.service.GenomeSerDe;
import net.lukemcomber.dev.ai.genetics.world.Ecosystem;
import net.lukemcomber.dev.ai.genetics.world.terrain.Terrain;
import net.lukemcomber.dev.ai.genetics.world.terrain.TerrainProperty;
import net.lukemcomber.oracle.model.*;
import net.lukemcomber.oracle.model.net.*;
import net.lukemcomber.oracle.service.WorldCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("genetics")
public class WorldController {

    private Logger logger = LoggerFactory.getLogger(WorldController.class);

    @Autowired
    private WorldCache cache;

    @PostMapping("v1.0/world")
    public ResponseEntity<CreateWorldResponse> createWorld(@RequestBody final CreateWorldRequest request) {

        final CreateWorldResponse response = new CreateWorldResponse();

        response.setStatusCode(HttpStatus.BAD_REQUEST);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.valueToTree(request);

        final Ecosystem system = new EcoSystemJsonReader().read(rootNode);
        if (null != system) {
            response.id = cache.set(system);
            response.setStatusCode(HttpStatus.OK);
        } else {
            response.setMessage("No terrain created.");
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
    public ResponseEntity<GenericResponse> advanceWorld(@PathVariable(name = "id") final String id,
                                                        @RequestParam(name = "turns", required = false, defaultValue = "1") final Integer turns) {

        //TODO if we switch to async, we'll need to fail if system is already advancing

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                for (int i = 0; i < turns; ++i) {
                    system.advance();
                }
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(new GenericResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return super.getStatusCode();
            }
        });
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
                        if(cell instanceof SeedCell){
                            location =new SeedCellLocation(spatialCoordinates.xAxis, spatialCoordinates.yAxis,
                                    spatialCoordinates.zAxis, cell.getCellType(), ((SeedCell)cell).isActivated());
                        } else {
                           location = new CellLocation(spatialCoordinates.xAxis, spatialCoordinates.yAxis,
                                   spatialCoordinates.zAxis, cell.getCellType());
                        }
                        body.addCell(location);
                    }
                    response.organismBodies.add(body);
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
                    response.addOccupantValue("energy cost", String.valueOf(cell.getMetabolismCost()));
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
}
