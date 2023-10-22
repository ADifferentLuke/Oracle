package net.lukemcomber.oracle.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lukemcomber.dev.ai.genetics.biology.Cell;
import net.lukemcomber.dev.ai.genetics.biology.Organism;
import net.lukemcomber.dev.ai.genetics.model.Coordinates;
import net.lukemcomber.dev.ai.genetics.service.CellHelper;
import net.lukemcomber.dev.ai.genetics.service.EcoSystemJsonReader;
import net.lukemcomber.dev.ai.genetics.world.Ecosystem;
import net.lukemcomber.dev.ai.genetics.world.terrain.Terrain;
import net.lukemcomber.oracle.model.CellInfo;
import net.lukemcomber.oracle.model.CreateWorldRequest;
import net.lukemcomber.oracle.model.World;
import net.lukemcomber.oracle.model.WorldSummary;
import net.lukemcomber.oracle.service.WorldCache;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("genetics")
public class WorldController {

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
            System.out.println("Bitch! I got a terrain! [" + id + "]");
        } else {
            System.out.println("No terrain created!");
        }
        //TODO handle error messages
        return "{ \"id\": \"" + id + "\"}";
    }

    @GetMapping("v1.0/world/{id}")
    public WorldSummary getWorldSummary(@PathVariable final String id) {
        final WorldSummary retVal = new WorldSummary();

        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system ){
                final Terrain terrain = system.getTerrain();
                if( null != terrain){
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
                              @RequestParam( name="turns", required = false, defaultValue = "1") final Integer turns ){
        final ObjectMapper mapper = new ObjectMapper();
        if (StringUtils.isNotEmpty(id)) {
            final Ecosystem system = cache.get(id);
            if (null != system) {
                for( int i = 0; i < turns; ++i ){
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
                final Iterator<Organism> iterator = system.getOrganisms();
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
}
