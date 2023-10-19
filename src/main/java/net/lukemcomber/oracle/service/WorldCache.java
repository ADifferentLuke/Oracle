package net.lukemcomber.oracle.service;

import net.lukemcomber.dev.ai.genetics.world.Ecosystem;
import net.lukemcomber.dev.ai.genetics.world.terrain.Terrain;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class WorldCache {

    private final HashMap<String, Ecosystem> universe;

    public WorldCache() {
        universe = new HashMap<>();
    }

    public String set(final Ecosystem system) {
        final String id = UUID.randomUUID().toString();
        universe.put(id, system);
        System.out.println("Set world cache: " + id);
        return id;
    }

    public Ecosystem get(final String id) {
        final Ecosystem system = universe.get(id);
        if (null == system) {
            System.out.println("Lookup failed: " + id);
        } else {
            System.out.println("Lookup success: " + id);
        }
        return system;
    }

}
