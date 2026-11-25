import { useEffect, useRef, useState } from "react";
import { api, errorMessage } from "../../api/client";
import { authApi } from "../../api/endpoints";

const SCRIPT_URL = "https://accounts.google.com/gsi/client";

/** Loads Google's sign-in script once. */
function loadGoogleScript() {
  if (window.google?.accounts?.id) return Promise.resolve();
  return new Promise((resolve, reject) => {
    let script = document.querySelector(`script[src="${SCRIPT_URL}"]`);
    if (!script) {
      script = document.createElement("script");
      script.src = SCRIPT_URL;
      script.async = true;
      document.head.appendChild(script);
    }
    script.addEventListener("load", () => resolve());
    script.addEventListener("error", () => reject(new Error("Google sign-in could not load")));
  });
}

/**
 * "Continue with Google" button. Google checks who the person is and gives us a token;
 * our backend verifies it with Google and signs them in (creating the account the first time).
 */
export function GoogleButton({ onSignedIn, text = "continue_with" }) {
  const box = useRef(null);
  const [error, setError] = useState("");
  const [hidden, setHidden] = useState(false);
  const onSignedInRef = useRef(onSignedIn);
  onSignedInRef.current = onSignedIn; // always call the latest version without re-running the setup

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { googleClientId } = await api("/api/auth/config");
        if (!googleClientId) return setHidden(true);
        await loadGoogleScript();
        if (cancelled || !box.current) return;
        window.google.accounts.id.initialize({
          client_id: googleClientId,
          callback: async ({ credential }) => {
            setError("");
            try {
              onSignedInRef.current(await authApi.google(credential));
            } catch (err) {
              setError(errorMessage(err));
            }
          },
        });
        window.google.accounts.id.renderButton(box.current, {
          theme: "outline",
          size: "large",
          shape: "pill",
          text,
          width: Math.min(box.current.offsetWidth || 320, 400),
        });
      } catch {
        if (!cancelled) setHidden(true); // e.g. Google blocked on this network: just hide the button
      }
    })();
    return () => { cancelled = true; };
  }, [text]);

  if (hidden) return null;
  return (
    <div className="google-signin">
      <div className="divider"><span>or</span></div>
      <div ref={box} className="google-button" />
      {error && <p className="field-error" role="alert">{error}</p>}
    </div>
  );
}
