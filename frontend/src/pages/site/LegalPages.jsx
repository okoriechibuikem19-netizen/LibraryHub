import { Link } from "react-router";

const UPDATED = "25 November 2026";

/** Privacy Policy: what LibraryHub stores and why. */
export function PrivacyPage() {
  return (
    <article className="page-container legal-page">
      <h1>Privacy Policy</h1>
      <p className="muted">Last updated: {UPDATED}</p>

      <p>
        LibraryHub is a free online library of public-domain novels and openly licensed textbooks. This page explains what
        information we keep about you, why, and what you can do about it.
      </p>

      <h2>What we collect</h2>
      <ul>
        <li><strong>Account details:</strong> your name and email address. If you set a password, we store only a secure
          scrambled version (a hash), never the password itself.</li>
        <li><strong>Google sign-in:</strong> if you choose "Continue with Google", Google tells us your name, email address
          and that the email is verified. We do not receive your Google password or access to anything else in your Google account.</li>
        <li><strong>Reading activity:</strong> the books you save, your bookmarks and how far you have read, so you can pick up where you stopped.</li>
        <li><strong>Basic technical data:</strong> your internet address may be used for a short time to block repeated
          wrong-password attempts, and our hosting provider keeps standard server logs.</li>
      </ul>

      <h2>Cookies and storage</h2>
      <ul>
        <li>A sign-in cookie keeps you logged in (it expires after 8 hours).</li>
        <li>A security cookie protects forms from being submitted by other websites.</li>
        <li>Your reader settings (theme, text size) are kept in your own browser.</li>
      </ul>
      <p>We do not use advertising or tracking cookies.</p>

      <h2>How we use it</h2>
      <p>
        Only to run the library: signing you in, saving your reading progress, keeping accounts secure and contacting you
        about your account if needed. We do not sell your information or share it for advertising.
      </p>

      <h2>Services we rely on</h2>
      <ul>
        <li><strong>Render</strong> hosts the website.</li>
        <li><strong>MongoDB Atlas</strong> stores account and reading data.</li>
        <li><strong>Google</strong> provides the optional "Continue with Google" sign-in.</li>
        <li>Book covers and texts are loaded from <strong>Project Gutenberg</strong>, <strong>Open Library / Internet Archive</strong> and
          <strong> OpenStax</strong>. When your browser loads a cover, those sites can see your internet address, as with any website.</li>
      </ul>

      <h2>Your choices</h2>
      <p>
        You can change your name and password on your <Link to="/account">Account</Link> page. To have your account and
        reading data deleted, contact the library administrator and we will remove it.
      </p>

      <h2>Children</h2>
      <p>LibraryHub is meant for general audiences. If you are under 13, please use it with a parent or guardian.</p>

      <h2>Changes</h2>
      <p>If we change this policy, we will update this page and the date above.</p>
    </article>
  );
}

/** Terms of Service: the rules for using LibraryHub. */
export function TermsPage() {
  return (
    <article className="page-container legal-page">
      <h1>Terms of Service</h1>
      <p className="muted">Last updated: {UPDATED}</p>

      <p>By using LibraryHub you agree to these terms. If you do not agree, please do not use the site.</p>

      <h2>The service</h2>
      <p>
        LibraryHub lets you browse, save and read free books. Novels come from Project Gutenberg and Open Library; textbooks
        come from OpenStax and other open sources. Some books open on the original source's website. The service is free and
        is provided "as is", without guarantees that it will always be available or error-free.
      </p>

      <h2>Your account</h2>
      <ul>
        <li>Give your real name and an email address you control.</li>
        <li>Keep your password private. You are responsible for activity on your account.</li>
        <li>We may suspend accounts that abuse the service, for example by spamming, trying to break in, or overloading the site.</li>
      </ul>

      <h2>Books and copyright</h2>
      <p>
        Books on LibraryHub are in the public domain or published under open licences by their sources. Licences can differ
        between countries; you are responsible for checking that your use is allowed where you live. Books borrowed through
        Open Library follow Open Library's own rules.
      </p>

      <h2>Acceptable use</h2>
      <p>
        Do not use LibraryHub to break the law, to harm other users, or to copy the site's data in bulk with automated tools.
      </p>

      <h2>Limitation of liability</h2>
      <p>
        To the extent the law allows, LibraryHub and its operators are not responsible for losses caused by using or being
        unable to use the site.
      </p>

      <h2>Changes</h2>
      <p>
        We may update these terms. Continuing to use LibraryHub after a change means you accept the new terms. See also our{" "}
        <Link to="/privacy">Privacy Policy</Link>.
      </p>
    </article>
  );
}
