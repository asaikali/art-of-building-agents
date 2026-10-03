#!/usr/bin/env bash

# shellcheck disable=SC1091
source "${MISE_PROJECT_ROOT:?}/.mise/lib/terminal.sh"

start_meal_agent_module() {
    local module="$1"
    shift
    local module_dir="${MISE_PROJECT_ROOT}/meal-agent/${module}"

    set_terminal_title "${module}"

    # Install the module and its in-repo dependencies (agent-core, restaurant-data)
    # so spring-boot:run picks up the current source instead of a stale local repository copy.
    "${MISE_PROJECT_ROOT}/mvnw" -q -B -f "${MISE_PROJECT_ROOT}/pom.xml" \
        -pl "meal-agent/${module}" -am install -DskipTests

    cd "${module_dir}"
    exec "${MISE_PROJECT_ROOT}/mvnw" spring-boot:run "$@"
}
