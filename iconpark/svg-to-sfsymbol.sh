convert_svg_to_sfsymbol() {
    local input_folder="$1"
    local output_folder="$2"

    mkdir -p "$output_folder"

    for svg_file in "$input_folder"/*.svg; do
        filename=$(basename "$svg_file" .svg)

        swiftdraw "$svg_file" --format sfsymbol > "$output_folder/$filename.sfsymbol"
        echo "Converted $svg_file to $output_folder/$filename.sfsymbol"

        symbol_svg_file="$input_folder/$filename-symbol.svg"
        if [ -f "$symbol_svg_file" ]; then
            mv "$symbol_svg_file" "$output_folder/"
            echo "Moved additional file $symbol_svg_file to $output_folder/"
        fi
    done

    echo "All .svg files in $input_folder have been converted to .sfsymbol format."
}

convert_svg_to_sfsymbol "build/icons/iconpark/outline" "Resources"