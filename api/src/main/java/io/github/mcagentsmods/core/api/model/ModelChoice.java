package io.github.mcagentsmods.core.api.model;

import java.util.Objects;

/**
 * A model, and the provider whose API it is called through.
 *
 * <p>The two are separate on purpose. The model is a stored name, {@code {platform}/{model}} as
 * {@link ModelName} keeps it, and says <em>which model</em>; the provider says <em>where the
 * request goes</em>. One name per model whatever the route is the naming rule's whole point,
 * so the route is a setting of its own rather than a spelling of the name:
 *
 * <table border="1">
 *   <caption>One model, two choices</caption>
 *   <tr><th>Provider</th><th>Model</th><th>Sent to</th><th>Named on the wire</th></tr>
 *   <tr><td>{@code openrouter}</td><td>{@code deepseek/deepseek-v4-flash}</td>
 *       <td>{@code openrouter.ai/api/v1}</td><td>{@code deepseek/deepseek-v4-flash}</td></tr>
 *   <tr><td>{@code deepseek}</td><td>{@code deepseek/deepseek-v4-flash}</td>
 *       <td>{@code api.deepseek.com/v1}</td><td>{@code deepseek-v4-flash}</td></tr>
 * </table>
 *
 * <p>See {@link ProviderRoute#to(ModelProvider, String)} for the rule that names the model on
 * a direct call.
 *
 * @param provider the provider id, lowercase; blank becomes {@link ModelPlatform#OPENROUTER}
 * @param model    the stored model name, lowercase, never blank
 */
public record ModelChoice(String provider, String model) {

    public ModelChoice {
        Objects.requireNonNull(model, "model");
        provider = ModelPlatform.normalise(provider);
        model = ModelName.normalise(model);
        if (model.isEmpty()) {
            throw new IllegalArgumentException("A model choice needs a model");
        }
    }

    /** {@code model}, called through OpenRouter. */
    public static ModelChoice throughOpenRouter(String model) {
        return new ModelChoice(ModelPlatform.OPENROUTER, model);
    }

    /**
     * {@code model} called at {@code provider}, with the platform added to an unqualified id.
     *
     * <p>A model typed without its platform for a direct provider is that provider's model, and
     * the naming rule never stores a name without its platform. Through OpenRouter an
     * unqualified id is left alone: the gateway serves no model of its own, so there is no
     * platform to add.
     */
    public static ModelChoice qualified(String provider, String model) {
        String owner = ModelPlatform.normalise(provider);
        String name = ModelName.normalise(Objects.requireNonNull(model, "model"));
        if (!ModelPlatform.isOpenRouter(owner) && !name.isEmpty() && !ModelName.isQualified(name)) {
            name = ModelName.of(owner, name).qualified();
        }
        return new ModelChoice(owner, name);
    }

    /** Whether the request goes through the gateway. */
    public boolean isThroughOpenRouter() {
        return ModelPlatform.isOpenRouter(provider);
    }

    /** This choice with another model, at the same provider. */
    public ModelChoice withModel(String model) {
        return new ModelChoice(provider, model);
    }

    /** This choice with another provider, for the same model. */
    public ModelChoice withProvider(String provider) {
        return new ModelChoice(provider, model);
    }

    /**
     * {@code own} when a task set one, else {@code fallback}: the one fallback rule.
     *
     * @param own      the task's own choice, or {@code null} when it set none
     * @param fallback what to use instead, never {@code null}
     */
    public static ModelChoice orElse(ModelChoice own, ModelChoice fallback) {
        return own != null ? own : Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public String toString() {
        return model + " @ " + provider;
    }
}
