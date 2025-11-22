#!/bin/sh
# This fix does not work: user:set not working
# n8n user:set --email "admin@n8n.io" --password "$N8N_BASIC_AUTH_PASSWORD" --update-current false --user "$N8N_BASIC_AUTH_USER"

# Folder to import from
IMPORT_FOLDER="/home/node/.n8n/import"

# Function to import workflow if not existing
import_workflow() {
  file="$1"
  workflow_name=$(jq -r '.name' "$file")

  if [ -z "$workflow_name" ]; then
    echo "No workflow name found in $file, skipping."
    return
  fi

  # Check if workflow already exists
  EXISTING=$(n8n list:workflow --active false | grep -i "$workflow_name" || true)

  if [ -z "$EXISTING" ]; then
    echo "Importing workflow: $workflow_name"
    n8n import:workflow --input="$file"
  else
    echo "Workflow already exists: $workflow_name — skipping import."
  fi
}

# Wait a bit for database to be ready
echo "🕒 Waiting for n8n internal DB to be ready..."
sleep 5

# Import all workflows
if [ -d "$IMPORT_FOLDER" ]; then
  for file in "$IMPORT_FOLDER"/*.json; do
    import_workflow "$file"
  done
fi

# Now start n8n normally
echo "Starting n8n..."
n8n start
