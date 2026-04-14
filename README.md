# Sortify

Sortify is a full-stack AI-powered `.txt` file sorter that turns messy plain-text files into organized, reviewable, and exportable categories.

It was built around a simple problem: notes, reminders, records, tasks, and mixed text data often end up scattered across plain `.txt` files. Sortify lets a user upload one or more files, classify each line through a validated AI pipeline, review the grouped results, edit categories when needed, and export either per-file outputs or one merged result.

## Why I Built It

I wanted to build a project that combined:

- a polished React frontend
- a Spring Boot backend with validation and error handling
- real OpenAI API integration
- practical product decisions around privacy, editing, and export behavior
- a maintainable AI workflow instead of a one-off prompt demo

The goal was to make a focused full-stack tool that feels useful, reliable, and understandable from both the user side and the engineering side.

## What It Does

- Upload one or more `.txt` files
- Drag and drop files into the app
- Categorize unstructured text with the OpenAI API
- Show grouped results per file
- Show one merged grouped view across all uploaded files
- Search through categorized results
- Edit categories after processing without re-uploading files
- Download one sorted file per upload
- Download one merged export
- Toggle optional browser persistence for files and results
- Configure theme, visual effects, privacy, limits, and access-token behavior from Settings

## Tech Stack

**Frontend**

- React
- Vite
- CSS

**Backend**

- Java
- Spring Boot
- REST API

**AI Integration**

- OpenAI API

## Engineering Highlights

This project is more than an upload form connected to an AI prompt. The current implementation focuses on predictable behavior and maintainable structure.

- Fixed category schema  
  The backend uses a centralized category schema so the app does not rely on arbitrary model-generated labels.

- Strict AI output validation  
  The parser requires one classified output per input line, validates categories, rejects missing or modified lines, and keeps the final results aligned with the original text.

- Shared text-file processing  
  File reading and line cleaning are centralized so upload preview and AI processing use the same behavior.

- Multi-file and merged output  
  Each uploaded file gets its own organized result, and the app also creates one combined grouped view across all uploaded files.

- Post-processing edits  
  Users can reassign categories after sorting, and the merged results plus downloads stay in sync with those edits.

- Privacy-aware persistence  
  Browser-side file/result caching is optional instead of automatic, which is safer for sensitive files.

- Upload and API protection  
  The backend enforces file count, per-file size, and total upload size limits to reduce accidental misuse and API overconsumption.

- Dependency/security cleanup  
  Frontend and backend dependency advisories were addressed, including Vite and Jackson updates.

## How It Works

1. A user uploads or drags in one or more `.txt` files from the React frontend.
2. The frontend sends those files to the Spring Boot backend.
3. The backend validates file count, file size, total upload size, and file type.
4. The backend reads the text, trims blank lines, and sends the cleaned lines to OpenAI.
5. OpenAI classifies each line into the supported category schema.
6. The backend validates the AI response before accepting it.
7. The frontend renders grouped results per file and one merged view across all files.
8. The user can search, expand/collapse sections, edit categories, and export cleaned outputs.

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
- Optional browser persistence for files/results
- Optional access-token protection for local/private use
- Backend upload limits and structured error responses

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
git clone https://github.com/ibeanny/Sortify.git
cd Sortify
```

### 2. Configure environment variables

Set your OpenAI API key:

```powershell
setx OPENAI_API_KEY "YOUR_API_KEY_HERE"
```

Then restart your terminal or IDE so the variable is available to the app.

Optional local access token:

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

## Testing

Backend tests can be run with:

```bash
./gradlew test
```

Frontend production build can be checked with:

```bash
cd frontend
npm run build
```

## Security Notes

- Uploaded text is sent to OpenAI for classification
- Browser-side file/result persistence is opt-in, not automatic
- The backend enforces file-count and upload-size limits
- AI output is validated against a fixed category schema
- Optional access-token protection can be enabled for private/local use
- Local secrets are loaded from environment variables and should not be committed

## What I Would Improve Next

- Add screenshots and a short demo video for stronger project presentation
- Expand automated test coverage across more frontend paths
- Add support for additional file formats beyond `.txt`
- Add stronger multi-user authentication before any public deployment

## Portfolio Notes

This project demonstrates:

- full-stack application development
- frontend state management and UI interaction design
- backend API design and validation
- OpenAI API integration
- AI response validation and schema enforcement
- privacy/security-oriented product decisions
- iterative feature development from prototype to polished tool

## Author

Elvis Ortiz  
Computer Science Student
