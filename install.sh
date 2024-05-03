#!/bin/bash

INSTALL_DIR="/opt/correct_script.cli"

REPO="iyubondyrev/correct_script.cli"

API_URL="https://api.github.com/repos/$REPO/releases/latest"

echo "Creating installation directory at $INSTALL_DIR..."
sudo mkdir -p "$INSTALL_DIR"

echo "Fetching latest release from GitHub..."
release_data=$(curl -s "$API_URL")

jar_url=$(echo "$release_data" | grep "browser_download_url.*correct_script.cli.jar" | cut -d '"' -f 4)

echo "Downloading correct_script.cli.jar..."
sudo curl -L "$jar_url" -o "$INSTALL_DIR/correct_script.cli.jar"

if [ -f "$HOME/.bashrc" ]; then
    PROFILE="$HOME/.bashrc"
elif [ -f "$HOME/.zshrc" ]; then
    PROFILE="$HOME/.zshrc"
else
    echo "No .bashrc or .zshrc found, using .bashrc by default"
    PROFILE="$HOME/.bashrc"
fi

echo "Adding alias to the shell profile..."
echo "alias correct_script='java -jar $INSTALL_DIR/correct_script.cli.jar 2> /dev/null'" >> $PROFILE

echo "Installation completed. Please run 'source $PROFILE' to reload your shell or restart your terminal."

