package io.github.mcagentsmods.core.common.web;

import io.github.mcagentsmods.core.api.web.NavLink;
import io.github.mcagentsmods.core.api.web.WebModule;
import io.github.mcagentsmods.core.api.web.WebRequest;
import io.github.mcagentsmods.core.api.web.WebResponse;

import java.util.List;
import java.util.Optional;

/**
 * Core's own pages: the front page, the shared shell, and the navigation every header is
 * built from.
 *
 * <p>Registered under the reserved id {@code core}, so it answers {@code /core/},
 * {@code /assets/core/…} and {@code /api/core/…} through the same routing as any mod's
 * module, and no mod can take those paths.
 */
final class ShellModule implements WebModule {

    static final String ID = "core";

    private final CoreWebRegistry registry;

    ShellModule(CoreWebRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "MCAgents";
    }

    @Override
    public List<NavLink> navigation() {
        return List.of();
    }

    @Override
    public Optional<String> page(String path) {
        return path.isEmpty() ? Optional.of("pages/index.html") : Optional.empty();
    }

    @Override
    public String resourceRoot() {
        return "mcagents_core/web/";
    }

    @Override
    public WebResponse handle(WebRequest request) {
        if (request.is("GET", "navigation")) {
            return WebResponse.ok(navigation(registry.registeredModules()));
        }
        return WebResponse.error(404, "No such endpoint.");
    }

    /**
     * Every registered module and its links, for the header and the front page.
     *
     * <p>{@code {"modules":[{"id","title","home","links":[{"label","href","section"}]}]}}.
     * Every string goes through {@link WebResponse#quote(String)}, so a module's title cannot
     * break out of the JSON it is written into.
     */
    static String navigation(List<WebModule> modules) {
        StringBuilder json = new StringBuilder("{\"modules\":[");
        for (int m = 0; m < modules.size(); m++) {
            WebModule module = modules.get(m);
            if (m > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(WebResponse.quote(module.id()))
                    .append(",\"title\":").append(WebResponse.quote(module.title()))
                    .append(",\"home\":").append(WebResponse.quote(home(module)))
                    .append(",\"links\":[");
            List<NavLink> links = module.navigation();
            for (int l = 0; l < links.size(); l++) {
                NavLink link = links.get(l);
                if (l > 0) {
                    json.append(',');
                }
                json.append("{\"label\":").append(WebResponse.quote(link.label()))
                        .append(",\"href\":")
                        .append(WebResponse.quote("/" + module.id() + "/" + link.path()))
                        .append(",\"section\":").append(WebResponse.quote(link.section()))
                        .append('}');
            }
            json.append("]}");
        }
        return json.append("]}").toString();
    }

    /** Where a module's first link goes, or its root when it has none. */
    static String home(WebModule module) {
        List<NavLink> links = module.navigation();
        return "/" + module.id() + "/" + (links.isEmpty() ? "" : links.getFirst().path());
    }
}
