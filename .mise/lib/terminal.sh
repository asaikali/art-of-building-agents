#!/usr/bin/env bash

set_terminal_title() {
    # Set the terminal tab title using OSC; Bash's -t check keeps escape codes out of logs.
    if [[ -t 1 ]]; then
        printf '\033]0;%s\007' "$1"
    fi
}
