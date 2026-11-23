import { useState, type ChangeEvent, type FormEvent } from "react";
import { Sidebar } from "@/features/dashboard/components/Sidebar";
import { Topbar } from "@/features/dashboard/components/Topbar";
import { apiClient } from "@/services/apiClient";

type GoogleStorageTestPageProps = {
  onBackToDashboard: () => void;
  displayName: string;
  role: "ADMIN" | "OPERATIONS";
};

type GoogleStorageTestResponse = {
  bucketName: string;
  objectName: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
};

function formatBytes(sizeBytes: number) {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`;
  }

  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`;
  }

  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function GoogleStorageTestPage({
  onBackToDashboard,
  displayName,
  role,
}: GoogleStorageTestPageProps) {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<GoogleStorageTestResponse | null>(null);

  function updateFile(event: ChangeEvent<HTMLInputElement>) {
    setFile(event.target.files?.[0] ?? null);
    setError("");
    setResult(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setError("Select a file before submitting.");
      return;
    }

    setUploading(true);
    setError("");
    setResult(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await apiClient.postForm<GoogleStorageTestResponse>(
        "/google-storage-test/upload",
        formData
      );
      setResult(response);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Upload failed.");
    } finally {
      setUploading(false);
    }
  }

  return (
    <main className="dashboard-page">
      <Sidebar
        activeItem="Google Test"
        onDashboard={onBackToDashboard}
        onGoogleTest={() => undefined}
        role={role}
      />
      <div className="dashboard-shell">
        <Topbar
          breadcrumb={["Reports", "Google Test"]}
          displayName={displayName}
          role={role}
          showSearch={false}
        />
        <section className="google-test-workspace">
          <header className="google-test-header">
            <div>
              <h1>Google Storage Test</h1>
              <p>Upload one file directly to the configured Google Cloud Storage bucket.</p>
            </div>
            <button
              className="customer-button customer-button--secondary"
              type="button"
              onClick={onBackToDashboard}
            >
              <span className="material-symbols-outlined">arrow_back</span>
              Back
            </button>
          </header>

          <form className="google-test-panel" onSubmit={submit}>
            <label className="google-test-upload">
              <span className="google-test-upload__icon material-symbols-outlined">
                upload_file
              </span>
              <span className="google-test-upload__content">
                <strong>Test File</strong>
                <small>PDF, JPG, or PNG. Max 10MB.</small>
                <span>{file ? file.name : "No file selected"}</span>
              </span>
              <span className="google-test-upload__button">
                Choose File
                <input
                  accept="application/pdf,image/jpeg,image/png"
                  type="file"
                  onChange={updateFile}
                />
              </span>
            </label>

            {error ? (
              <p className="customer-form-message customer-form-message--error" role="alert">
                {error}
              </p>
            ) : null}

            {result ? (
              <dl className="google-test-result" aria-label="Upload result">
                <div>
                  <dt>Bucket</dt>
                  <dd>{result.bucketName}</dd>
                </div>
                <div>
                  <dt>Object</dt>
                  <dd>{result.objectName}</dd>
                </div>
                <div>
                  <dt>File</dt>
                  <dd>{result.originalFilename}</dd>
                </div>
                <div>
                  <dt>Size</dt>
                  <dd>{formatBytes(result.sizeBytes)}</dd>
                </div>
              </dl>
            ) : null}

            <footer className="google-test-actions">
              <button
                className="customer-button customer-button--outline"
                disabled={uploading}
                type="submit"
              >
                <span className="material-symbols-outlined">cloud_upload</span>
                {uploading ? "Uploading..." : "Submit"}
              </button>
            </footer>
          </form>
        </section>
      </div>
    </main>
  );
}
