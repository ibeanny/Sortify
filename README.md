# Sortify

Sortify is a full-stack application that transforms unstructured text files into organized, categorized data using AI.

Upload one or more messy `.txt` files, and Sortify analyzes and structures the content into readable sections.

## Features

- Upload one or more `.txt` files
- Drag and drop `.txt` files into the web app
- AI-powered categorization of unstructured data
- Generates structured and readable results per file
- Builds one combined result across all uploaded files
- Download one sorted `.txt` per uploaded file or one combined export
- Gear-based settings panel for theme, privacy, access options, and optional visual effects
- Clean output formatting
- Expand or collapse each file result and the combined result
- Search across sorted categories and lines
- Edit line categories after sorting without re-uploading files
- Real-time processing through a full-stack system
- Upload limits to protect the backend and API usage
- Optional browser-side persistence instead of always caching sensitive files
- Optional access token protection for local/private deployments

## How It Works

1. The user uploads or drags in one or more `.txt` files through the React frontend
2. The files are sent to a Spring Boot backend via an API
3. The backend processes each file line-by-line
4. Each line is analyzed and categorized using OpenAI
5. The backend validates the AI output against a fixed category schema
6. The results are grouped into structured sections for each file
7. The frontend also builds a combined grouped view across all uploaded files
8. The user can search, expand, collapse, edit categories, and download the organized output

## Tech Stack

Frontend:
- React
- Vite
- CSS

Backend:
- Java
- Spring Boot
- REST API

AI Integration:
- OpenAI API

## Setup

### Clone the repository

git clone https://github.com/ibeanny/sortify.git
cd sortify

### Configure API Key

Set an environment variable:

PowerShell:

`setx OPENAI_API_KEY "YOUR_API_KEY_HERE"`

Then restart your terminal or IDE.

Create a file:

src/main/resources/application.properties

Add:

spring.application.name=aisorter
openai.api.key=${OPENAI_API_KEY}
sortify.upload.max-files=10
sortify.upload.max-file-size-bytes=1048576
sortify.upload.max-total-upload-bytes=4194304
sortify.security.access-token=${SORTIFY_ACCESS_TOKEN:}

`SORTIFY_ACCESS_TOKEN` is optional. If set, the web app will ask for it before uploads and processing.

### Run Backend

Run the Spring Boot application in IntelliJ  
or run:

./gradlew bootRun

### Run Frontend

cd frontend  
npm install  
npm run dev

Open the Vite app in your browser and:

- choose multiple `.txt` files from the picker, or
- drag and drop multiple `.txt` files into the upload panel
- optionally open the settings panel to enable "Remember files and results on this browser" if the files are not sensitive
- optionally turn on decorative visual effects in settings
- use the search bar to filter categories or lines in the results
- use "Edit Categories" to reassign lines after sorting if needed

After processing, Sortify shows:

- one collapsible result box per file
- one final combined result box for all uploaded files
- a search field for filtering the results view
- an edit mode for changing categories after sorting
- a button to download one sorted `.txt` per uploaded file
- a button to download one combined `.txt`

## Security Notes

- Uploaded text is sent to OpenAI for classification
- Browser-side file/result persistence is opt-in, not automatic
- You can require a local access token with `SORTIFY_ACCESS_TOKEN`
- The backend enforces file-count and upload-size limits
- The backend validates AI categories against a fixed schema before returning results

## Future Improvements

- Enhance UI/UX design
- Support additional file formats
- Add stronger user authentication for multi-user deployments

## Author

Elvis Ortiz  
Computer Science Student :D
