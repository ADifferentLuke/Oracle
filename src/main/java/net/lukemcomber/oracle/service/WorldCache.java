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
    private Set<Map<String, Ecosystem>> linkedUniverses;

    public WorldCache() {
        universe = new HashMap<>();
        linkedUniverses = new HashSet<>();
    }

    public void setLinkedUniversesReference( final Map<String,Ecosystem> reference){
        if(! linkedUniverses.contains(reference)) {
            linkedUniverses.add(reference);
        }
    }

    public String set(final Ecosystem system) {
        universe.put(system.getId(), system);
        return system.getId();
    }


    public Ecosystem get(final String id) {
        Ecosystem ecosystem;

        ecosystem = universe.get(id);
        if( null == ecosystem ){
            final Iterator<Map<String, Ecosystem>> iter = linkedUniverses.iterator();
            while( iter.hasNext() ){
                final Map<String, Ecosystem> map = iter.next();
                if( map.containsKey(id)){
                    ecosystem = map.get(id);
                    break;
                }
            }
        }
        return ecosystem;
    }

    public Collection<Ecosystem> values(){
        final LinkedList<Ecosystem> compiledList = new LinkedList<>(universe.values());
        final Iterator<Map<String, Ecosystem>> iterator = linkedUniverses.iterator();
        while( iterator.hasNext()){
            final Map<String, Ecosystem> map = iterator.next();
            compiledList.addAll( map.values());
        }
        return compiledList;
    }


}
