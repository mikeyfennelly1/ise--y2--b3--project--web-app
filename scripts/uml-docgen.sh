#!/usr/bin/env bash

PLANTUML_DIR="$HOME/.plantuml"
PLANTUML_JAR="$PLANTUML_DIR/plantuml.jar"

install_plantuml() {
    echo "Installing PlantUML dependencies..."

    sudo apt install -y graphviz

    echo "Downloading PlantUML..."

    mkdir -p "$PLANTUML_DIR"

    curl -L \
        https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar \
        -o "$PLANTUML_JAR"

    echo "PlantUML installed at $PLANTUML_JAR"
}

render_puml_to_jpg() {
    INPUT_FILE="$1"

    if [ -z "$INPUT_FILE" ]; then
        echo "Usage: render_puml_to_jpg <file.puml>"
        return 1
    fi

    if [ ! -f "$INPUT_FILE" ]; then
        echo "File not found: $INPUT_FILE"
        return 1
    fi

    if [ ! -f "$PLANTUML_JAR" ]; then
        echo "PlantUML not installed. Run install_plantuml first."
        return 1
    fi

    echo "Rendering $INPUT_FILE → JPG"

    java -jar "$PLANTUML_JAR" -tjpg "$INPUT_FILE"
}
