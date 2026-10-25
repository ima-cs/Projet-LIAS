export async function api(url, options = {}) {
  const token = sessionStorage.getItem("lias-token");
  const headers = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };
  if (options.body && !(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
    options = { ...options, body: JSON.stringify(options.body) };
  }
  const response = await fetch(url, { ...options, headers });
  if (!response.ok) {
    let data;
    try {
      data = await response.json();
    } catch {
      data = {};
    }
    const error = new Error(
      data.message ||
        {
          400: "Vérifiez les données saisies.",
          401: "Veuillez vous connecter à nouveau.",
          403: "Vous ne disposez pas des droits pour cette action.",
          404: "Élément introuvable.",
          409: "Cette opération entre en conflit avec les données existantes.",
        }[response.status] ||
        "Le serveur a rencontré une erreur.",
    );
    error.status = response.status;
    if (response.status === 401 && url != "/api/auth/login") {
      sessionStorage.removeItem("lias-token");
      window.dispatchEvent(new Event("lias-expired"));
    }
    throw error;
  }
  if (response.status === 204) return null;
  const type = response.headers.get("content-type") || "";
  if (type.includes("json")) return response.json();
  return response.blob();
}
export async function download(id, name = "document.pdf") {
  const blob = await api(`/api/documents/${id}/telecharger`);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
