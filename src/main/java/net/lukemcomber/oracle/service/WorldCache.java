package net.lukemcomber.oracle.service;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import net.lukemcomber.genetics.Ecosystem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class WorldCache {

    //TODO add a remove or clear
    private final HashMap<String, Ecosystem> universe;

    public WorldCache() {
        universe = new HashMap<>();
    }

    public String set(final Ecosystem system) {
        universe.put(system.getId(), system);
        return system.getId();
    }

    public Ecosystem get(final String id) {
        final Ecosystem system = universe.get(id);
        return system;
    }

    public Collection<Ecosystem> values(){
        return universe.values();
    }

}
