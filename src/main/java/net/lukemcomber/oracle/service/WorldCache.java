package net.lukemcomber.oracle.service;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import net.lukemcomber.genetics.Ecosystem;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class WorldCache {

    private final Map<String, Ecosystem> universe;

    public WorldCache() {
        universe = new HashMap<>();
    }

    public synchronized String set(final Ecosystem system) {
        universe.put(system.getId(), system);
        return system.getId();
    }


    public synchronized Ecosystem get(final String id) {
        return universe.get(id);
    }

    public synchronized Collection<Ecosystem> values(){
        return new LinkedList<>(universe.values());
    }

    public synchronized Ecosystem remove( final String id ){
        return universe.remove(id);
    }


}
