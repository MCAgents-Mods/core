package io.github.mcagentsmods.core.common.web;

import io.github.mcagentsmods.core.api.web.NavLink;
import io.github.mcagentsmods.core.api.web.WebModule;
import io.github.mcagentsmods.core.api.web.WebRequest;
import io.github.mcagentsmods.core.api.web.WebResponse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** A module the way a mod would write one: two pages, a stylesheet, and a small API. */
class SampleModule implements WebModule {

    private final String id;
    final AtomicReference<WebRequest> lastRequest = new AtomicReference<>();

    SampleModule(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return "Sample <b>mod</b>";
    }

    @Override
    public List<NavLink> navigation() {
        return List.of(new NavLink("Home", "home/"), new NavLink("Other", "other/"));
    }

    @Override
    public Optional<String> page(String path) {
        return path.equals("home/") ? Optional.of("pages/home.html") : Optional.empty();
    }

    @Override
    public String resourceRoot() {
        return "testweb/";
    }

    @Override
    public WebResponse handle(WebRequest request) {
        lastRequest.set(request);
        if (request.is("GET", "echo")) {
            return WebResponse.ok("{\"q\":" + WebResponse.quote(
                    request.parameter("q").orElse("")) + "}");
        }
        if (request.is("POST", "echo")) {
            return WebResponse.ok("{\"length\":" + request.body().length() + "}");
        }
        if (request.is("POST", "refuse")) {
            throw new IllegalArgumentException("That name is already taken.");
        }
        if (request.is("GET", "explode")) {
            throw new IllegalStateException("/home/player/secret/path");
        }
        return WebModule.super.handle(request);
    }
}
