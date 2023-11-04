package net.lukemcomber.oracle.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.dev.ai.genetics.biology.Cell;
import net.lukemcomber.dev.ai.genetics.biology.Gene;
import net.lukemcomber.dev.ai.genetics.biology.Genome;
import net.lukemcomber.dev.ai.genetics.biology.Organism;
import net.lukemcomber.dev.ai.genetics.model.Coordinates;
import net.lukemcomber.dev.ai.genetics.service.CellHelper;
import net.lukemcomber.dev.ai.genetics.service.EcoSystemJsonReader;
import net.lukemcomber.dev.ai.genetics.service.GenomeSerDe;
import net.lukemcomber.dev.ai.genetics.service.GenomeStdOutWriter;
import net.lukemcomber.dev.ai.genetics.world.Ecosystem;
import net.lukemcomber.dev.ai.genetics.world.terrain.Terrain;
import net.lukemcomber.dev.ai.genetics.world.terrain.TerrainProperty;
import net.lukemcomber.oracle.model.*;
import net.lukemcomber.oracle.service.WorldCache;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("genetics")
public class WorldController {

    private Logger logger = LoggerFactory.getLogger(WorldController.class);

    @Autowired
    private WorldCache cache;

    @GetMapping("v1.0/world/info")
    public String getWorldInformation() {
        return "No world running...";
    }

    //TODO switch to response entities
    @PostMapping("v1.0/world")
    public String createWorld(@RequestBody final CreateWorldRequest request) {

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode rootNode = mapper.valueToTree(request);
        String id = "";

        final Ecosystem system = new EcoSystemJsonReader().read(rootNode);
        if (null != system) {
            id = cache.set(system);
            logger.debug("Create world: " + id);
        } else {
            logger.warn("No terrain created!");
        }
        //TODO handle error messages
        return "{ \"id\": \"" + id + "\"}";
    }

    @GetMapping("v1.0/world/{id}")
    public WorldSummary getWorldSummary(@PathVariable final String id) {
        final WorldSummary retVal = new WorldSummary();

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                final Terrain terrain = system.getTerrain();
                if (null != terrain) {
                    retVal.width = terrain.getSizeOfXAxis();
                    retVal.height = terrain.getSizeOfYAxis();
                    retVal.depth = terrain.getSizeOfZAxis();

                    retVal.id = id;
                } else {
                    //TODO terrain not found
                    throw new NotImplementedException("Terrain not found");
                }
            } else {
                //TODO no world found
                throw new NotImplementedException("World not found");
            }
        }
        return retVal;
    }

    @GetMapping("v1.0/world/advance/{id}")
    public String advanceWorld(@PathVariable final String id,
                               @RequestParam(name = "turns", required = false, defaultValue = "1") final Integer turns) {
        final ObjectMapper mapper = new ObjectMapper();
        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                for (int i = 0; i < turns; ++i) {
                    system.advance();
                }
            }
        }
        return "{}";
    }

    @GetMapping("v1.0/world/details/{id}")
    public World showWorld(@PathVariable String id) {
        final ObjectMapper mapper = new ObjectMapper();
        final World world = new World();
        if (StringUtils.isNotEmpty(id)) {

            //TODO add color ids and move cell type to a details endpoint

            final Ecosystem system = cache.get(id);
            if (null != system) {
                world.currentTick = system.getCurrentTick();
                world.totalDays = system.getTotalDays();
                ;
                world.totalTicks = system.getTotalTicks();

                //get list of cells
                final Iterator<Organism> iterator = system.getTerrain().getOrganisms();
                while (iterator.hasNext()) {
                    for (final Cell cell : CellHelper.getAllOrganismsCells(iterator.next().getCells())) {
                        final Coordinates coordinates = cell.getCoordinates();
                        world.cells.add(new CellInfo(coordinates.xAxis, coordinates.yAxis, coordinates.zAxis, cell.getCellType()));
                    }
                }
            }
        }
        return world;
    }

    @GetMapping("v1.0/world/{worldId}/organisms")
    public List<String> getOrganisms(@PathVariable String worldId) {
        final List<String> retVal = new LinkedList<>();
        final Ecosystem ecosystem = cache.get(worldId);
        if (null != ecosystem) {
            ecosystem.getTerrain().getOrganisms().forEachRemaining(o -> retVal.add(o.getUniqueID()));
        } else {
            throw new RuntimeException("World not found: " + worldId);
        }
        return retVal;
    }

    @GetMapping("v1.0/world/{worldId}/inspect")
    public Map<String,String> getLocationInformation(@PathVariable String worldId,
                                                     @RequestParam(name = "x") final int xCoord,
                                                     @RequestParam(name = "y") final int yCoord) {
        final Map<String,String> retVal = new HashMap<>();
        final Ecosystem ecosystem = cache.get(worldId);
        if (null != ecosystem) {
           if( 0 <= xCoord && 0 <= yCoord ){
               //We don't do negative space
               final Coordinates location = new Coordinates(xCoord,yCoord,0);
               final List<TerrainProperty> properties = ecosystem.getTerrain().getTerrain(location);

               for( final TerrainProperty terrainProperty : properties ){
                    retVal.put(terrainProperty.getId(), terrainProperty.getValue().toString());
               }
               final Cell cell = ecosystem.getTerrain().getCell(location);
               if( null != cell ){
                   retVal.put( "cell", cell.getCellType());
                   retVal.put( "organism", cell.getOrganism().getUniqueID());
                   retVal.put( "energy cost", String.valueOf(cell.getMetabolismCost()));
               }
               retVal.put( "coordinates", location.toString() );
           } else {
               throw new RuntimeException(String.format("Invalid coordinates (%d,%d)",xCoord,yCoord));
           }
        } else {
            throw new RuntimeException("World not found: " + worldId);
        }
        return retVal;
    }

    @GetMapping("v1.0/world/{worldId}/organism/{organismId}")
    public OrganismDetails getOrganismsGenes(@PathVariable String worldId,
                                             @PathVariable String organismId) {


        OrganismDetails details = null;
        if (StringUtils.isNotEmpty(worldId) && StringUtils.isNotEmpty(organismId)) {
            final Ecosystem ecosystem = cache.get(worldId);
            if (null != ecosystem) {
                //TODO we can make this way way more efficient. F linear searches
                final Iterator<Organism> iter = ecosystem.getTerrain().getOrganisms();
                while (iter.hasNext()) {
                    final Organism organism = iter.next();
                    if (organism.getUniqueID().equals(organismId)) {
                        //we found our baby
                        details = new OrganismDetails();
                        details.age = null;
                        details.energy = organism.getEnergy();
                        details.genes = new LinkedList<>();
                        details.name = organismId;
                        details.genus = organism.getOrganismType();
                        details.parentId = organism.getParentId();
                        final Genome genome = organism.getGenome();
                        details.genome = GenomeSerDe.serialize(genome);

                        for (int i = 0; genome.getNumberOfGenes() > i; ++i) {
                            final Gene gene = genome.getGeneNumber(i);
                            if (null != gene) {
                                details.genes.add(gene);
                            }
                        }
                        break;
                    }
                }
            } else {
                throw new RuntimeException("World " + worldId + " not found.");
            }
        } else {
            throw new RuntimeException("World or Organism Id is null!");
        }
        return details;
    }
}
