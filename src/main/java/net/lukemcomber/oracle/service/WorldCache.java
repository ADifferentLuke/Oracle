package net.lukemcomber.oracle.service;

/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */


import net.lukemcomber.genetics.Ecosystem;
import net.lukemcomber.genetics.utilities.model.SimulationSessions;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class WorldCache {

    //TODO add a remove or clear
    private final HashMap<String, Ecosystem> universe;
    private final List<SimulationSessions> autoRunningSimulations;

    public WorldCache() {
        universe = new HashMap<>();
        autoRunningSimulations = new LinkedList<>();
    }

    public void addSimulationSession( final SimulationSessions simulationSessions ){
        autoRunningSimulations.add(simulationSessions);
    }

    public String set(final Ecosystem system) {
        universe.put(system.getId(), system);
        return system.getId();
    }

    private Ecosystem getFromSimulation(final String id){
        for( final SimulationSessions sessions : autoRunningSimulations){
           final Ecosystem ecosystem = sessions.get(id);
           if( null != ecosystem){
               return ecosystem;
           }
        }
        return null;
    }

    public Ecosystem get(final String id) {
        Ecosystem system = universe.get(id);
        if( null == system ){
            system = getFromSimulation(id);
        }
        return system;
    }

    public Collection<Ecosystem> values(){
        final Collection<Ecosystem> fullList = new LinkedList<>(universe.values());
        for( final SimulationSessions sessions : autoRunningSimulations){
           fullList.addAll(sessions.sessions());
        }
        return fullList;
    }


}
