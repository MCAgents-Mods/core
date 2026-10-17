package io.github.mcagentsmods.core.api.hologram;

/** One row of a {@link Form}. Each kind is a record; core draws it and handles its clicks. */
public sealed interface FormRow
        permits FormText, FormField, FormArea, FormToggle, FormStepper, FormButtons {
}
