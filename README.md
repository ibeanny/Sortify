# Sortify

Sortify is a full-stack web application that turns messy text files into organized, readable groups using AI.

It was built to solve a simple but useful problem: when notes, reminders, records, and mixed text data are scattered across plain `.txt` files, they are hard to review, combine, and export in a clean way. Sortify lets a user upload one or more text files, process them through an AI-powered categorization flow, review the grouped results, edit categories if needed, and download either per-file outputs or one merged export.

## Why I Built It

I wanted to build a project that combined:

- a modern frontend experience
- a backend API with validation and error handling
- real OpenAI API integration
- meaningful product decisions around privacy, editing, and output formatting

This project gave me a chance to work across the full stack instead of only building a UI or only building an API.

## What It Does

- Upload one or more `.txt` files
- Drag and drop files into the app
- Categorize unstructured text using the OpenAI API
- Group results per file and across all uploaded files
- Search through categorized results
- Edit categories after processing without re-uploading files
- Download one sorted file per upload or one merged export
- Toggle browser persistence for saved files/results
- Configure theme and optional visual effects from settings

## Tech Stack

### Frontend

- React
- Vite
- CSS

### Backend

- Java
- Spring Boot
- REST API

### AI Integration

- OpenAI API

## Engineering Highlights

This project is more than a basic upload form connected to an AI prompt. A few parts I focused on:

- Fixed-schema AI categorization  
  The backend validates AI output against a supported category schema instead of blindly trusting arbitrary model labels.

- Multi-file processing and merged output  
  Each uploaded file gets its own organized result, and the app also produces one combined grouped view across all files.

- Post-processing edits in the UI  
  Users can reassign categories after sorting, and the merged results/downloads stay in sync with those edits.

- Privacy-aware persistence  
  Browser-side file/result caching is optional instead of automatic, which is safer for sensitive text.

- Upload and API protection  
  The backend enforces file count and upload size limits to prevent misuse and accidental API overconsumption.

- Structured error handling  
  The frontend and backend use cleaner error messages so failures are easier to understand and debug.

## How It Works

1. A user uploads or drags in one or more `.txt` files from the React frontend.
2. The frontend sends those files to the Spring Boot backend.
3. The backend validates file count, file size, total upload size, and file type.
4. The backend trims and processes text lines, then sends them to OpenAI for categorization.
5. AI output is validated against a fixed category schema before being accepted.
6. The frontend renders grouped results for each file plus a combined view across all files.
7. The user can search results, expand/collapse sections, edit categories, and export cleaned outputs.

## Current Features

- Multi-file `.txt` uploads
- Drag-and-drop upload support
- AI-powered categorization of unstructured text
- Per-file grouped results
- Combined grouped results across all files
- Search/filter inside the results view
- Edit mode for changing categories after sorting
- Download one sorted `.txt` per uploaded file
- Download one merged `.txt` across all uploaded files
- Settings panel for theme, privacy, limits, and optional visual effects
- Optional access-token protection for local/private use

## Project Structure

```text
sortify/
├── frontend/                 # React + Vite frontend
├── src/main/java/            # Spring Boot backend
├── src/main/resources/       # Application config
└── src/test/java/            # Backend tests
```

## Running Locally

### 1. Clone the repository

```bash
git clone https://github.com/ibeanny/sortify.git
cd sortify
```

### 2. Configure environment variables

Set your OpenAI API key:

```powershell
setx OPENAI_API_KEY "YOUR_API_KEY_HERE"
```

Then restart your terminal or IDE so the variable is available to the app.

Optional:

```powershell
setx SORTIFY_ACCESS_TOKEN "YOUR_LOCAL_ACCESS_TOKEN"
```

If `SORTIFY_ACCESS_TOKEN` is set, the frontend will prompt for it before processing uploads.

### 3. Create `application.properties`

Create:

```text
src/main/resources/application.properties
```

Add:

```properties
spring.application.name=aisorter
openai.api.key=${OPENAI_API_KEY}
sortify.upload.max-files=10
sortify.upload.max-file-size-bytes=1048576
sortify.upload.max-total-upload-bytes=4194304
sortify.security.access-token=${SORTIFY_ACCESS_TOKEN:}
```

### 4. Run the backend

In IntelliJ, run the Spring Boot application, or use:

```bash
./gradlew bootRun
```

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Then open the local Vite URL in your browser.

## Security Notes

- Uploaded text is sent to OpenAI for classification
- Browser-side file/result persistence is opt-in, not automatic
- The backend enforces file-count and upload-size limits
- AI output is validated against a fixed category schema
- Optional access-token protection can be enabled for private/local deployments

## What I’d Improve Next

- Add support for additional file formats beyond `.txt`
- Expand automated test coverage across more frontend and backend paths
- Add a stronger multi-user authentication model for public deployment
- Add screenshots and a short demo video for better project presentation

## Portfolio Notes

This project demonstrates:

- full-stack application development
- frontend state management and UI interaction design
- backend API design and validation
- OpenAI API integration
- privacy/security-oriented product decisions
- iterative feature development from a working prototype into a more polished tool

## Author

Elvis Ortiz  
Computer Science Student
