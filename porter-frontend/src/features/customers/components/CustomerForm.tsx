import { useState, type ChangeEvent, type FormEvent } from "react";
import { apiClient } from "@/services/apiClient";

type CustomerAccountResponse = {
  id: number;
  companyName: string;
};

type CustomerFormProps = {
  createdBy: string;
  onCancel: () => void;
};

type CustomerFormState = {
  companyName: string;
  companyRegistrationNumber: string;
  vatNumber: string;
  nationalAddress: string;
  pocContactNumber: string;
  pocEmailAddress: string;
  commsEmailInput: string;
  commsEmailList: string[];
};

type CustomerDocuments = {
  registrationDocument: File | null;
  vatDocument: File | null;
  nationalAddressDocument: File | null;
};

type DocumentField = keyof CustomerDocuments;
type TextCustomerFormField = Exclude<keyof CustomerFormState, "commsEmailList">;

const initialFormState: CustomerFormState = {
  companyName: "",
  companyRegistrationNumber: "",
  vatNumber: "",
  nationalAddress: "",
  pocContactNumber: "",
  pocEmailAddress: "",
  commsEmailInput: "",
  commsEmailList: [],
};

const initialDocuments: CustomerDocuments = {
  registrationDocument: null,
  vatDocument: null,
  nationalAddressDocument: null,
};

function RequiredMark() {
  return <span className="required-mark">*</span>;
}

function OptionalMark() {
  return <span className="customer-attachment-row__optional">Optional</span>;
}

function appendIfPresent(formData: FormData, key: string, file: File | null) {
  if (file) {
    formData.append(key, file);
  }
}

function fileName(file: File | null) {
  return file?.name ?? "No file selected";
}

function DocumentUploadCard({
  accept = "application/pdf,image/jpeg",
  file,
  field,
  title,
  onChange,
}: {
  accept?: string;
  file: File | null;
  field: DocumentField;
  title: string;
  onChange: (field: DocumentField, event: ChangeEvent<HTMLInputElement>) => void;
}) {
  const attached = Boolean(file);

  return (
    <label
      className={`customer-attachment-row${
        attached ? " customer-attachment-row--attached" : ""
      }`}
    >
      <span className="customer-attachment-row__icon material-symbols-outlined">
        {attached ? "check_circle" : "description"}
      </span>
      <span className="customer-attachment-row__content">
        <strong className="customer-attachment-row__title">
          {title} <OptionalMark />
        </strong>
        <small>PDF, JPG</small>
        <small className={`customer-attachment-row__filename${
          file ? "" : " customer-attachment-row__filename--empty"
        }`}>
          {fileName(file)}
        </small>
      </span>
      <span className="customer-attachment-row__browse">
        {attached ? "Change" : "Add"}
        <input
          accept={accept}
          type="file"
          onChange={(event) => onChange(field, event)}
        />
      </span>
    </label>
  );
}

export function CustomerForm({
  createdBy,
  onCancel,
}: CustomerFormProps) {
  const [form, setForm] = useState<CustomerFormState>(initialFormState);
  const [documents, setDocuments] = useState<CustomerDocuments>(initialDocuments);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [createdAccount, setCreatedAccount] = useState<CustomerAccountResponse | null>(null);

  function updateField(field: TextCustomerFormField, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function updateDocument(field: keyof CustomerDocuments, event: ChangeEvent<HTMLInputElement>) {
    setDocuments((current) => ({
      ...current,
      [field]: event.target.files?.[0] ?? null,
    }));
  }

  function addCommsEmail() {
    const email = form.commsEmailInput.trim().toLowerCase();
    if (!email || form.commsEmailList.includes(email)) {
      updateField("commsEmailInput", "");
      return;
    }

    setForm((current) => ({
      ...current,
      commsEmailInput: "",
      commsEmailList: [...current.commsEmailList, email],
    }));
  }

  function removeCommsEmail(email: string) {
    setForm((current) => ({
      ...current,
      commsEmailList: current.commsEmailList.filter((item) => item !== email),
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setCreatedAccount(null);

    const formData = new FormData();
    formData.append("companyName", form.companyName);
    formData.append("companyRegistrationNumber", form.companyRegistrationNumber);
    formData.append("vatNumber", form.vatNumber);
    formData.append("nationalAddress", form.nationalAddress);
    formData.append("pocContactNumber", form.pocContactNumber);
    formData.append("pocEmailAddress", form.pocEmailAddress);
    form.commsEmailList.forEach((email) => {
      formData.append("commsEmailList", email);
    });
    formData.append("createdBy", createdBy);
    appendIfPresent(formData, "registrationDocument", documents.registrationDocument);
    appendIfPresent(formData, "vatDocument", documents.vatDocument);
    appendIfPresent(formData, "nationalAddressDocument", documents.nationalAddressDocument);

    try {
      const account = await apiClient.postForm<CustomerAccountResponse>(
        "/customer-accounts",
        formData
      );
      setCreatedAccount(account);
    } catch (exception) {
      const message = exception instanceof Error
        ? exception.message
        : "Could not create the customer account. Check the required fields.";
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  const completedDocuments = Object.values(documents).filter(Boolean).length;

  return (
    <form className="customer-workflow-form" onSubmit={handleSubmit}>
      <div className="customer-workflow-form__body">
        <div className="customer-workflow-form__main">
          <section className="customer-panel customer-panel--identity">
            <header className="customer-panel__header">
              <div>
                <h2>Company details</h2>
              </div>
            </header>

            <div className="customer-form-grid">
              <label className="customer-input customer-input--wide">
                <span>
                  Company Name <RequiredMark />
                </span>
                <div className="customer-input__control">
                  <span className="material-symbols-outlined">domain</span>
                  <input
                    required
                    placeholder="Acme Logistics"
                    value={form.companyName}
                    onChange={(event) => updateField("companyName", event.target.value)}
                  />
                </div>
              </label>

              <label className="customer-input">
                <span>
                  Registration Number <RequiredMark />
                </span>
                <div className="customer-input__control">
                  <span className="material-symbols-outlined">badge</span>
                  <input
                    required
                    placeholder="CR-12345"
                    value={form.companyRegistrationNumber}
                    onChange={(event) =>
                      updateField("companyRegistrationNumber", event.target.value)
                    }
                  />
                </div>
              </label>

              <label className="customer-input">
                <span>
                  VAT Number <RequiredMark />
                </span>
                <div className="customer-input__control">
                  <span className="material-symbols-outlined">receipt_long</span>
                  <input
                    required
                    placeholder="VAT-98765"
                    value={form.vatNumber}
                    onChange={(event) => updateField("vatNumber", event.target.value)}
                  />
                </div>
              </label>
            </div>
          </section>

          <section className="customer-panel customer-panel--contact">
            <header className="customer-panel__header">
              <div>
                <h2>Contact details</h2>
              </div>
            </header>

            <div className="customer-form-grid">
              <label className="customer-input">
                <span>
                  POC Contact Number <RequiredMark />
                </span>
                <div className="customer-input__control">
                  <span className="material-symbols-outlined">call</span>
                  <input
                    required
                    placeholder="0512345678"
                    type="tel"
                    value={form.pocContactNumber}
                    onChange={(event) => updateField("pocContactNumber", event.target.value)}
                  />
                </div>
              </label>

              <label className="customer-input">
                <span>
                  POC Email Address <RequiredMark />
                </span>
                <div className="customer-input__control">
                  <span className="material-symbols-outlined">mail</span>
                  <input
                    required
                    placeholder="contact@example.com"
                    type="email"
                    value={form.pocEmailAddress}
                    onChange={(event) => updateField("pocEmailAddress", event.target.value)}
                  />
                </div>
              </label>

              <label className="customer-input customer-input--wide">
                <span>Comms Email List</span>
                <div className="customer-tag-input">
                  <div className="customer-tag-input__entry">
                    <input
                      placeholder="finance@example.com"
                      type="email"
                      value={form.commsEmailInput}
                      onChange={(event) => updateField("commsEmailInput", event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === ",") {
                          event.preventDefault();
                          addCommsEmail();
                        }
                      }}
                    />
                    <button type="button" onClick={addCommsEmail}>
                      Add
                    </button>
                  </div>
                  {form.commsEmailList.length ? (
                    <div className="customer-tag-list" aria-label="Comms emails">
                      {form.commsEmailList.map((email) => (
                        <span className="customer-tag" key={email}>
                          {email}
                          <button
                            aria-label={`Remove ${email}`}
                            type="button"
                            onClick={() => removeCommsEmail(email)}
                          >
                            <span className="material-symbols-outlined">close</span>
                          </button>
                        </span>
                      ))}
                    </div>
                  ) : null}
                </div>
              </label>
            </div>
          </section>

          <section className="customer-panel customer-panel--address">
            <header className="customer-panel__header">
              <div>
                <h2>Location</h2>
              </div>
            </header>

            <div className="customer-form-grid">
              <label className="customer-input customer-input--full">
                <span>
                  National Address <RequiredMark />
                </span>
                <textarea
                  required
                  placeholder="Riyadh 12211"
                  rows={4}
                  value={form.nationalAddress}
                  onChange={(event) => updateField("nationalAddress", event.target.value)}
                />
              </label>
            </div>
          </section>
        </div>

        <aside className="customer-workflow-form__rail" aria-label="Account setup status">
          <section className="customer-rail-panel customer-rail-panel--progress">
            <header>
              <span className="material-symbols-outlined">fact_check</span>
              <h2>Setup Status</h2>
            </header>
            <dl className="customer-status-list">
              <div>
                <dt>Created by</dt>
                <dd>{createdBy}</dd>
              </div>
              <div>
                <dt>Initial status</dt>
                <dd>CREATED</dd>
              </div>
              <div>
                <dt>Documents</dt>
                <dd>{completedDocuments}/3 attached</dd>
              </div>
            </dl>
          </section>

          <section className="customer-rail-panel">
            <header>
              <span className="material-symbols-outlined">upload_file</span>
              <h2>Documents</h2>
            </header>
            <div className="customer-document-stack">
              <DocumentUploadCard
                file={documents.registrationDocument}
                field="registrationDocument"
                title="Registration Document"
                onChange={updateDocument}
              />
              <DocumentUploadCard
                file={documents.vatDocument}
                field="vatDocument"
                title="VAT Document"
                onChange={updateDocument}
              />
              <DocumentUploadCard
                file={documents.nationalAddressDocument}
                field="nationalAddressDocument"
                title="National Address Document"
                onChange={updateDocument}
              />
            </div>
          </section>
        </aside>

        {error ? (
          <p className="customer-form-message customer-form-message--error" role="alert">
            {error}
          </p>
        ) : null}
        {createdAccount ? (
          <p className="customer-form-message customer-form-message--success" role="status">
            Customer account created for {createdAccount.companyName}.
          </p>
        ) : null}
      </div>

      <footer className="customer-workflow-form__footer">
        <button
          className="customer-button customer-button--secondary"
          type="button"
          onClick={onCancel}
        >
          Cancel
        </button>
        <button
          className="customer-button customer-button--primary"
          disabled={submitting}
          type="submit"
        >
          Create Customer
        </button>
      </footer>
    </form>
  );
}
