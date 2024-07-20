package net.lukemcomber.oracle.model.net;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.lukemcomber.oracle.model.BasicResponse;

import java.util.List;

public class WorldsListResponse extends BasicResponse {

    public static class WorldOverview {
        @JsonProperty("id")
        public String id;

        @JsonProperty("active")
        public boolean active;

        @JsonProperty("steppable")
        public boolean steppable;
    }

    @JsonProperty("worlds")
    public List<WorldOverview> worlds;
}
