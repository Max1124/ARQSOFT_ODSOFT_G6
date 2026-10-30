#!/usr/bin/env bash
# Installs the post-commit hook into this clone. Run once from the repository root: jenkins/hooks/install.sh
set -euo pipefail
root="$(git rev-parse --show-toplevel)"
cp "${root}/jenkins/hooks/post-commit" "${root}/.git/hooks/post-commit"
chmod +x "${root}/.git/hooks/post-commit"
echo "Installed ${root}/.git/hooks/post-commit"
