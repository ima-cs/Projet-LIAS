import React, { useState, useEffect, useContext, createContext } from "react";
import { createRoot } from "react-dom/client";
import {
  BrowserRouter,
  Routes,
  Route,
  Link,
  NavLink,
  Navigate,
  useNavigate,
  useParams,
  useLocation,
} from "react-router-dom";
import { api, download } from "./api";
import "./style.css";

const Auth = createContext();
const today = () => new Date().toLocaleDateString("en-CA");
const label = (s) =>
  s
    ?.replaceAll("_", " ")
    .toLowerCase()
    .replace(/^./, (c) => c.toUpperCase());
function useData(url) {
  const [data, setData] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(true);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let active = true;
    setBusy(true);
    setError("");
    api(url)
      .then((d) => active && setData(d))
      .catch((e) => active && setError(e.message))
      .finally(() => active && setBusy(false));
    return () => {
      active = false;
    };
  }, [url, revision]);
  return { data, error, busy, reload: () => setRevision((x) => x + 1) };
}
function Status({ busy, error }) {
  return (
    <>
      {busy && (
        <p className="muted" role="status">
          Chargement…
        </p>
      )}
      {error && (
        <p role="alert" className="alert">
          {error}
        </p>
      )}
    </>
  );
}
function Badge({ children }) {
  return <span className="badge">{label(String(children || ""))}</span>;
}
function Page({ title, description, actions, children }) {
  return (
    <>
      <div className="page-head">
        <div>
          <div className="eyebrow">LABORATOIRE LIAS</div>
          <h1>{title}</h1>
          {description && <p className="muted">{description}</p>}
        </div>
        {actions}
      </div>
      {children}
    </>
  );
}
function Form({
  fields,
  initial = {},
  onSubmit,
  onCancel,
  submit = "Enregistrer",
}) {
  const [values, setValues] = useState(initial),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await onSubmit(values);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={save} className="form panel">
      <div className="form-grid">
        {fields.map((f) => (
          <label key={f.name} className={f.type === "textarea" ? "wide" : ""}>
            {f.label}
            {f.required && " *"}
            {f.type === "file" ? (
              <input
                type="file"
                accept="application/pdf"
                onChange={(e) =>
                  setValues({ ...values, [f.name]: e.target.files[0] })
                }
              />
            ) : f.type === "textarea" ? (
              <textarea
                maxLength={f.max || 10000}
                required={f.required}
                value={values[f.name] ?? ""}
                onChange={(e) =>
                  setValues({ ...values, [f.name]: e.target.value })
                }
              />
            ) : f.options ? (
              <select
                required={f.required}
                value={values[f.name] ?? ""}
                onChange={(e) =>
                  setValues({ ...values, [f.name]: e.target.value })
                }
              >
                <option value="">Sélectionner…</option>
                {f.options.map((o) => (
                  <option
                    key={typeof o === "string" ? o : o.value}
                    value={typeof o === "string" ? o : o.value}
                  >
                    {typeof o === "string" ? label(o) : o.label}
                  </option>
                ))}
              </select>
            ) : (
              <input
                type={f.type || "text"}
                min={f.min}
                max={f.max}
                minLength={f.minLength}
                maxLength={f.maxLength || 300}
                required={f.required}
                value={values[f.name] ?? ""}
                onChange={(e) =>
                  setValues({ ...values, [f.name]: e.target.value })
                }
              />
            )}
          </label>
        ))}
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        <button disabled={busy}>{busy ? "Enregistrement…" : submit}</button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel}>
            Annuler
          </button>
        )}
      </div>
    </form>
  );
}
const field = (name, label, type = "text", required = true, options) => ({
  name,
  label,
  type,
  required,
  options,
});
const opts = (list) =>
  list?.map((x) => ({
    value: x.id,
    label: x.nom || x.titre || x.libelle || x.designation,
  })) || [];
function Table({ rows, columns, actions }) {
  if (!rows?.length)
    return <div className="empty panel">Aucun résultat pour le moment.</div>;
  return (
    <div className="table-wrap panel">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
            {actions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r.id || r.membreId || i}>
              {columns.map((c) => (
                <td key={c.key}>
                  {c.render ? c.render(r) : String(r[c.key] ?? "—")}
                </td>
              ))}
              {actions && (
                <td>
                  <div className="row-actions">{actions(r)}</div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function App() {
  const [me, setMe] = useState(null),
    [loading, setLoading] = useState(true);
  const refresh = async () => {
    if (sessionStorage.getItem("lias-token"))
      try {
        setMe(await api("/api/auth/me"));
      } catch {
        setMe(null);
      }
    setLoading(false);
  };
  useEffect(() => {
    refresh();
    const expire = () => setMe(null);
    window.addEventListener("lias-expired", expire);
    return () => window.removeEventListener("lias-expired", expire);
  }, []);
  const has = (role) => me?.autorites.includes("ROLE_" + role);
  const logout = () => {
    sessionStorage.removeItem("lias-token");
    setMe(null);
  };
  if (loading)
    return <div className="startup">LIAS · Chargement de votre espace…</div>;
  return (
    <Auth.Provider value={{ me, has, refresh, logout }}>
      <BrowserRouter>
        <Layout />
      </BrowserRouter>
    </Auth.Provider>
  );
}
function Layout() {
  const { me, has, logout } = useContext(Auth);
  const { pathname } = useLocation();
  const [count, setCount] = useState(0);
  useEffect(() => {
    if (!me) return;
    const tick = () =>
      api("/api/notifications/non-lues")
        .then((d) => setCount(d.nombre))
        .catch(() => {});
    tick();
    const timer = setInterval(tick, 15000);
    return () => clearInterval(timer);
  }, [me, pathname]);
  const internal = has("PERMANENT");
  const nav = me
    ? [
        ["/dashboard", "Vue d’ensemble"],
        ["/profil", "Mon profil"],
        ["/membres", "Annuaire"],
        ["/equipes", "Équipes"],
        ["/publications", "Publications"],
        ["/evenements", "Événements"],
        ["/calendrier", "Calendrier"],
        ...(internal
          ? [
              ["/documents", "Documents"],
              ["/reunions", "Réunions & PV"],
              ["/materiel", "Inventaire"],
              ["/besoins", "Besoins matériels"],
              ["/partenaires", "Partenaires"],
              ["/conventions", "Conventions"],
              ["/rapports", "Rapports annuels"],
            ]
          : []),
        ["/discussions", "Discussions"],
        ["/notifications", `Notifications ${count ? "(" + count + ")" : ""}`],
        ...(has("DIRECTEUR")
          ? [
              ["/adhesions", "Adhésions"],
              ["/gouvernance", "Gouvernance"],
            ]
          : []),
        ...(has("ADMIN")
          ? [
              ["/comptes", "Comptes"],
              ["/gouvernance", "Historisation"],
            ]
          : []),
      ]
    : [
        ["/", "Accueil"],
        ["/equipes", "Équipes"],
        ["/membres", "Chercheurs"],
        ["/evenements", "Événements"],
        ["/publications", "Publications"],
        ["/adhesion", "Nous rejoindre"],
      ];
  return (
    <div className={me ? "app-shell" : "public-shell"}>
      {me ? (
        <aside>
          <Link className="brand" to="/dashboard">
            <span className="brand-icon">L</span> LIAS
            <span className="brand-dot">.</span>
          </Link>
          <div className="sidebar-caption">ESPACE DE RECHERCHE</div>
          <nav>
            {nav.map(([to, text]) => (
              <NavLink key={to} to={to}>
                {text}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <strong>
              {me.profil.prenom} {me.profil.nom}
            </strong>
            <small>
              {me.autorites.map((x) => label(x.slice(5))).join(" · ") ||
                "Administrateur"}
            </small>
            <button className="secondary" onClick={logout}>
              Se déconnecter
            </button>
          </div>
        </aside>
      ) : (
        <header className="public-header">
          <Link className="brand" to="/">
            LIAS<span className="brand-dot">.</span>
          </Link>
          <nav>
            {nav.map(([to, text]) => (
              <NavLink key={to} to={to}>
                {text}
              </NavLink>
            ))}
          </nav>
          <Link className="button" to="/connexion">
            Espace membre ↗
          </Link>
        </header>
      )}
      <div className="workspace">
        {me && (
          <div className="topbar">
            <span>Recherche, savoir & collaboration</span>
            <Link to="/recherche">⌕ Rechercher</Link>
            <Link to="/notifications" className="avatar" title="Notifications">
              {me.profil.prenom[0]}
              {me.profil.nom[0]}
            </Link>
          </div>
        )}
        <main>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route
              path="/connexion"
              element={me ? <Navigate to="/dashboard" /> : <Login />}
            />
            <Route path="/activation" element={<Activate />} />
            <Route path="/adhesion" element={<AdhesionForm />} />
            <Route path="/equipes" element={<Teams />} />
            <Route path="/membres" element={<Members />} />
            <Route path="/membres/:id" element={<Member />} />
            <Route path="/evenements" element={<Catalog kind="evenements" />} />
            <Route
              path="/publications"
              element={<Catalog kind="publications" />}
            />
            <Route
              path="/dashboard"
              element={
                <Protected>
                  <Dashboard />
                </Protected>
              }
            />
            <Route
              path="/profil"
              element={
                <Protected>
                  <Member self />
                </Protected>
              }
            />
            <Route
              path="/calendrier"
              element={
                <Protected>
                  <Calendar />
                </Protected>
              }
            />
            <Route
              path="/documents"
              element={
                <Protected role="PERMANENT">
                  <Documents />
                </Protected>
              }
            />
            <Route
              path="/reunions"
              element={
                <Protected role="PERMANENT">
                  <Meetings />
                </Protected>
              }
            />
            <Route
              path="/materiel"
              element={
                <Protected role="PERMANENT">
                  <Material />
                </Protected>
              }
            />
            <Route
              path="/besoins"
              element={
                <Protected role="PERMANENT">
                  <Needs />
                </Protected>
              }
            />
            <Route
              path="/partenaires"
              element={
                <Protected role="PERMANENT">
                  <Partners />
                </Protected>
              }
            />
            <Route
              path="/conventions"
              element={
                <Protected role="PERMANENT">
                  <Agreements />
                </Protected>
              }
            />
            <Route
              path="/rapports"
              element={
                <Protected role="PERMANENT">
                  <Reports />
                </Protected>
              }
            />
            <Route
              path="/adhesions"
              element={
                <Protected role="DIRECTEUR">
                  <Admissions />
                </Protected>
              }
            />
            <Route
              path="/gouvernance"
              element={
                <Protected any={["ADMIN", "DIRECTEUR"]}>
                  <Governance />
                </Protected>
              }
            />
            <Route
              path="/comptes"
              element={
                <Protected role="ADMIN">
                  <Accounts />
                </Protected>
              }
            />
            <Route
              path="/discussions"
              element={
                <Protected>
                  <Discussions />
                </Protected>
              }
            />
            <Route
              path="/notifications"
              element={
                <Protected>
                  <Notifications />
                </Protected>
              }
            />
            <Route
              path="/recherche"
              element={
                <Protected>
                  <Search />
                </Protected>
              }
            />
            <Route
              path="*"
              element={
                <Page title="Page introuvable">
                  <Link to="/">Revenir à l’accueil</Link>
                </Page>
              }
            />
          </Routes>
        </main>
        <footer>
          LIAS · Laboratoire de recherche{" "}
          <span>Faculté des Sciences Ben M’Sik · Casablanca</span>
        </footer>
      </div>
    </div>
  );
}
function Protected({ children, role, any }) {
  const { me, has } = useContext(Auth);
  if (!me) return <Navigate to="/connexion" replace />;
  if ((role && !has(role)) || (any && !any.some(has)))
    return (
      <Page
        title="Accès réservé"
        description="Votre compte ne dispose pas des autorisations nécessaires."
      />
    );
  return children;
}
function Login() {
  const { refresh } = useContext(Auth);
  return (
    <Page
      title="Bienvenue dans votre espace"
      description="Connectez-vous à la plateforme du laboratoire."
    >
      <div className="narrow">
        <Form
          fields={[
            field("email", "Adresse e-mail", "email"),
            field("motDePasse", "Mot de passe", "password"),
          ]}
          submit="Se connecter"
          onSubmit={async (v) => {
            const r = await api("/api/auth/login", { method: "POST", body: v });
            sessionStorage.setItem("lias-token", r.accessToken);
            await refresh();
          }}
        />
      </div>
    </Page>
  );
}
function Activate() {
  const [done, setDone] = useState(false);
  const invitationToken =
    new URLSearchParams(useLocation().search).get("token") || "";
  return (
    <Page
      title="Activer votre compte"
      description="Définissez votre mot de passe à l’aide de votre invitation."
    >
      {done ? (
        <p className="success">
          Compte activé. <Link to="/connexion">Se connecter</Link>
        </p>
      ) : (
        <div className="narrow">
          <Form
            initial={{
              token: invitationToken,
            }}
            fields={[
              ...(!invitationToken
                ? [field("token", "Jeton d’invitation")]
                : []),
              {
                ...field(
                  "motDePasse",
                  "Mot de passe (8 caractères minimum)",
                  "password",
                ),
                minLength: 8,
              },
              field("confirmation", "Confirmer le mot de passe", "password"),
            ]}
            onSubmit={async (v) => {
              if (v.motDePasse !== v.confirmation)
                throw new Error("Les mots de passe ne correspondent pas.");
              await api("/api/auth/activer", {
                method: "POST",
                body: { token: v.token, motDePasse: v.motDePasse },
              });
              setDone(true);
            }}
            submit="Activer le compte"
          />
        </div>
      )}
    </Page>
  );
}
function AdhesionForm() {
  const [done, setDone] = useState(false);
  return (
    <Page
      title="Rejoindre le LIAS"
      description="Présentez votre projet scientifique. La direction étudiera votre candidature."
    >
      {done ? (
        <div className="success panel">
          Votre demande a été enregistrée et sera étudiée par la direction.
        </div>
      ) : (
        <div className="narrow">
          <Form
            fields={[
              field("nom", "Nom"),
              field("prenom", "Prénom"),
              field("email", "Adresse e-mail", "email"),
              field(
                "motivation",
                "Motivation et projet de recherche",
                "textarea",
              ),
              field("cv", "CV (PDF, facultatif)", "file", false),
            ]}
            submit="Soumettre ma candidature"
            onSubmit={async (v) => {
              if (v.cv) {
                const body = new FormData();
                const { cv, ...demande } = v;
                body.append(
                  "demande",
                  new Blob([JSON.stringify(demande)], {
                    type: "application/json",
                  }),
                );
                body.append("file", cv);
                await api("/api/public/adhesions", { method: "POST", body });
              } else
                await api("/api/public/adhesions", { method: "POST", body: v });
              setDone(true);
            }}
          />
        </div>
      )}
    </Page>
  );
}
function Home() {
  const r = useData("/api/public/portail");
  return (
    <>
      <section className="hero">
        <div>
          <div className="eyebrow">RECHERCHE · INNOVATION · IMPACT</div>
          <h1>
            La recherche se
            <br />
            construit <em>ensemble.</em>
          </h1>
          <p>{r.data?.laboratoire.presentation}</p>
          <div className="row-actions">
            <Link className="button" to="/equipes">
              Découvrir nos équipes ↗
            </Link>
            <Link className="button secondary" to="/adhesion">
              Rejoindre le laboratoire
            </Link>
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="orbit one" />
          <div className="orbit two" />
          <div className="orbit three" />
          <span>
            LIAS<span className="brand-dot">.</span>
          </span>
          <small>CONNECTER LES SAVOIRS</small>
        </div>
      </section>
      <Status {...r} />
      {r.data && (
        <>
          <div className="stats">
            {[
              ["Équipes de recherche", r.data.equipes.length],
              ["Publications scientifiques", r.data.publications.length],
              ["Événements & rencontres", r.data.evenements.length],
            ].map(([s, n]) => (
              <div className="panel stat" key={s}>
                <strong>{n}</strong>
                <span>{s}</span>
              </div>
            ))}
          </div>
          <div className="section-title">
            <h2>Nos axes de recherche</h2>
            <Link to="/equipes">Toutes les équipes →</Link>
          </div>
          <div className="cards">
            {r.data.equipes.map((e, i) => (
              <article className="panel card" key={e.id}>
                <div className="card-number">0{i + 1}</div>
                <h2>{e.nom}</h2>
                <p>{e.description}</p>
                <Badge>{e.thematique}</Badge>
              </article>
            ))}
          </div>
          <div className="section-title">
            <h2>La vie du laboratoire</h2>
            <Link to="/evenements">Voir les événements →</Link>
          </div>
          <Table
            rows={r.data.evenements.slice(0, 3)}
            columns={[
              { key: "titre", label: "Événement" },
              { key: "dateDebut", label: "Date" },
              { key: "lieu", label: "Lieu" },
            ]}
          />
        </>
      )}
    </>
  );
}
function Dashboard() {
  const { me, has } = useContext(Auth);
  const r = useData("/api/public/portail");
  const n = useData("/api/notifications");
  return (
    <Page
      title={`Bonjour ${me.profil.prenom},`}
      description="Votre laboratoire, vos activités et vos prochains rendez-vous."
    >
      <Status {...r} />
      {r.data && (
        <>
          <div className="stats">
            {[
              ["Publications", r.data.publications.length, "/publications"],
              ["Équipes", r.data.equipes.length, "/equipes"],
              ["Événements", r.data.evenements.length, "/evenements"],
              [
                "Notifications non lues",
                n.data?.filter((x) => !x.lu).length || 0,
                "/notifications",
              ],
            ].map(([s, num, to]) => (
              <Link className="panel stat" key={s} to={to}>
                <span>{s} ↗</span>
                <strong>{num}</strong>
                <small>Consulter</small>
              </Link>
            ))}
          </div>
          <div className="dashboard-banner">
            <div className="eyebrow">VOTRE ESPACE COLLABORATIF</div>
            <h2>Faire avancer la recherche, chaque jour.</h2>
            <p>
              Retrouvez vos collègues, partagez vos travaux et préparez les
              prochains temps forts du LIAS.
            </p>
            <Link
              className="button"
              to={has("DIRECTEUR") ? "/adhesions" : "/profil"}
            >
              {has("DIRECTEUR")
                ? "Examiner les candidatures"
                : "Mettre à jour mon profil"}{" "}
              →
            </Link>
          </div>
          <div className="section-title">
            <h2>Prochains rendez-vous</h2>
            <Link to="/calendrier">Ouvrir le calendrier →</Link>
          </div>
          <Table
            rows={r.data.evenements
              .filter((e) => e.dateDebut >= today())
              .reverse()}
            columns={[
              { key: "titre", label: "Événement" },
              {
                key: "typeEvenement",
                label: "Type",
                render: (r) => <Badge>{r.typeEvenement}</Badge>,
              },
              { key: "dateDebut", label: "Date" },
              { key: "lieu", label: "Lieu" },
            ]}
          />
        </>
      )}
    </Page>
  );
}
function Teams() {
  const { me, has } = useContext(Auth);
  const r = useData(`/api/${me ? "" : "public/"}equipes`);
  const [form, setForm] = useState(null),
    [detail, setDetail] = useState(null),
    [error, setError] = useState("");
  return (
    <Page
      title="Équipes de recherche"
      description="Des expertises complémentaires au service d’une recherche commune."
      actions={
        has("DIRECTEUR") && (
          <button onClick={() => setForm({})}>+ Nouvelle équipe</button>
        )
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {form && (
        <Form
          key={form.id || "new"}
          initial={form}
          fields={[
            field("nom", "Nom"),
            field("thematique", "Thématique"),
            field("description", "Description", "textarea", false),
          ]}
          onCancel={() => setForm(null)}
          onSubmit={async (v) => {
            await api("/api/equipes" + (form.id ? "/" + form.id : ""), {
              method: form.id ? "PUT" : "POST",
              body: v,
            });
            setForm(null);
            r.reload();
          }}
        />
      )}
      <div className="cards">
        {r.data?.map((e, i) => (
          <article className="panel card" key={e.id}>
            <div className="card-number">0{i + 1}</div>
            <h2>{e.nom}</h2>
            <p>{e.description}</p>
            <Badge>{e.thematique}</Badge>
            <div className="row-actions">
              <button
                className="secondary"
                onClick={async () => {
                  try {
                    setDetail(
                      await api(`/api/${me ? "" : "public/"}equipes/${e.id}`),
                    );
                  } catch (e) {
                    setError(e.message);
                  }
                }}
              >
                Voir les membres
              </button>
              {has("DIRECTEUR") && (
                <button className="secondary" onClick={() => setForm(e)}>
                  Modifier
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
      {detail && (
        <section className="panel card">
          <h2>{detail.nom} · Membres</h2>
          {detail.membres.map((m) => (
            <p key={m.id}>
              <Link to={"/membres/" + m.id}>
                {m.prenom} {m.nom}
              </Link>{" "}
              · {label(m.statut)}
            </p>
          ))}
          {!detail.membres.length && <p>Aucun membre actuel.</p>}
        </section>
      )}
    </Page>
  );
}
function Members() {
  const { me } = useContext(Auth);
  const [q, setQ] = useState(""),
    [statut, setStatut] = useState(""),
    [team, setTeam] = useState(""),
    [page, setPage] = useState(0);
  const ts = useData("/api/public/equipes");
  const r = useData(
    `/api/${me ? "" : "public/"}membres?q=${encodeURIComponent(q)}&page=${page}&size=12${me && statut ? "&statut=" + statut : ""}${me && team ? "&equipeId=" + team : ""}`,
  );
  return (
    <Page
      title="Annuaire des chercheurs"
      description="Découvrez les personnes qui font vivre le laboratoire."
    >
      <div className="filters">
        <input
          placeholder="Rechercher un nom…"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
        />
        {me && (
          <>
            <select
              value={statut}
              onChange={(e) => {
                setStatut(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Tous les statuts</option>
              {["PERMANENT", "ASSOCIE", "DOCTORANT", "RETRAITE", "ANCIEN"].map(
                (x) => (
                  <option key={x}>{x}</option>
                ),
              )}
            </select>
            <select
              value={team}
              onChange={(e) => {
                setTeam(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Toutes les équipes</option>
              {ts.data?.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nom}
                </option>
              ))}
            </select>
          </>
        )}
      </div>
      <Status {...r} />
      <div className="cards">
        {r.data?.content.map((m) => (
          <Link
            className="panel card member-card"
            key={m.id}
            to={"/membres/" + m.id}
          >
            <div className="member-avatar">
              {m.photoUrl ? (
                <img src={m.photoUrl} alt="" />
              ) : (
                m.prenom[0] + m.nom[0]
              )}
            </div>
            <h2>
              {m.prenom} {m.nom}
            </h2>
            <Badge>{m.statut || "Administration"}</Badge>
            <p>{m.equipe || m.etablissementOrigine}</p>
            <span>Consulter le profil →</span>
          </Link>
        ))}
      </div>
      {r.data && (
        <div className="pagination">
          <button
            className="secondary"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Précédent
          </button>
          <span>
            Page {page + 1} · {r.data.totalElements} membres
          </span>
          <button
            className="secondary"
            disabled={page + 1 >= r.data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Suivant
          </button>
        </div>
      )}
    </Page>
  );
}
function Member({ self }) {
  const { me, has } = useContext(Auth);
  const params = useParams();
  const id = self ? me?.profil.membreId : params.id;
  const r = useData(`/api/${me ? "" : "public/"}membres/${id}`);
  const [edit, setEdit] = useState(false),
    [message, setMessage] = useState(""),
    [history, setHistory] = useState(null),
    [date, setDate] = useState(today()),
    [state, setState] = useState(null);
  const editable = me && (Number(id) === me.profil.membreId || has("ADMIN"));
  const fields = [
    field("nom", "Nom"),
    field("prenom", "Prénom"),
    field("biographie", "Biographie", "textarea", false),
    field("dateNaissance", "Date de naissance (confidentielle)", "date", false),
    field("dateEmbauche", "Date d’embauche", "date", false),
    field("etablissementOrigine", "Établissement d’origine", "text", false),
    field("laboratoireOrigine", "Laboratoire d’origine", "text", false),
    field(
      "interets",
      "Centres d’intérêt (séparés par des virgules)",
      "text",
      false,
    ),
  ];
  return (
    <Page
      title={self ? "Mon profil" : "Fiche chercheur"}
      actions={
        editable && (
          <button onClick={() => setEdit(!edit)}>Modifier mon profil</button>
        )
      }
    >
      <Status {...r} />
      {message && <p className="success">{message}</p>}
      {r.data &&
        (edit ? (
          <Form
            initial={{ ...r.data, interets: r.data.centresInteret.join(", ") }}
            fields={fields}
            onCancel={() => setEdit(false)}
            onSubmit={async (v) => {
              await api("/api/membres/" + id, {
                method: "PUT",
                body: {
                  ...v,
                  dateNaissance: v.dateNaissance || null,
                  dateEmbauche: v.dateEmbauche || null,
                  centresInteret:
                    v.interets
                      ?.split(",")
                      .map((s) => s.trim())
                      .filter(Boolean) || [],
                },
              });
              setEdit(false);
              setMessage("Profil enregistré. Une révision a été créée.");
              r.reload();
            }}
          />
        ) : (
          <section className="panel card">
            <div className="member-avatar large">
              {r.data.photoUrl ? (
                <img src={r.data.photoUrl} alt="Photo de profil" />
              ) : (
                r.data.prenom[0] + r.data.nom[0]
              )}
            </div>
            <h2>
              {r.data.prenom} {r.data.nom}
            </h2>
            <Badge>{r.data.statut || "Administration"}</Badge>
            <p>{r.data.biographie}</p>
            <dl>
              {[
                ["Équipe", r.data.equipe],
                ["Établissement", r.data.etablissementOrigine],
                ["Laboratoire", r.data.laboratoireOrigine],
                ["Date d’embauche", r.data.dateEmbauche],
                ["E-mail", r.data.email],
                ["Date de naissance (privée)", r.data.dateNaissance],
              ]
                .filter(([, v]) => v)
                .map(([k, v]) => (
                  <React.Fragment key={k}>
                    <dt>{k}</dt>
                    <dd>{v}</dd>
                  </React.Fragment>
                ))}
            </dl>
            <div className="row-actions">
              {r.data.centresInteret.map((s) => (
                <Badge key={s}>{s}</Badge>
              ))}
            </div>
            {editable && (
              <>
                <label className="file-label">
                  Changer la photo (PNG/JPEG)
                  <input
                    type="file"
                    accept="image/png,image/jpeg"
                    onChange={async (e) => {
                      if (!e.target.files[0]) return;
                      try {
                        const body = new FormData();
                        body.append("file", e.target.files[0]);
                        await api(`/api/membres/${id}/photo`, {
                          method: "PUT",
                          body,
                        });
                        r.reload();
                        setMessage("Photo enregistrée.");
                      } catch (e) {
                        setMessage(e.message);
                      }
                    }}
                  />
                </label>
                <button
                  className="secondary"
                  onClick={async () => {
                    try {
                      setHistory(await api(`/api/membres/${id}/historique`));
                    } catch (e) {
                      setMessage(e.message);
                    }
                  }}
                >
                  Historique des modifications
                </button>
              </>
            )}
          </section>
        ))}
      {history && (
        <Table
          rows={history}
          columns={[
            { key: "revision", label: "Révision" },
            {
              key: "date",
              label: "Date",
              render: (r) => new Date(r.date).toLocaleString("fr-FR"),
            },
            { key: "nom", label: "Nom" },
            { key: "biographie", label: "Biographie" },
          ]}
        />
      )}{" "}
      {me && (
        <section className="panel card">
          <h2>État du membre à une date</h2>
          <div className="filters">
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
            <button
              onClick={async () => {
                try {
                  setState(await api(`/api/membres/${id}/etat?date=${date}`));
                } catch (e) {
                  setMessage(e.message);
                }
              }}
            >
              Reconstituer
            </button>
          </div>
          {state && (
            <p>
              {state.date} ·{" "}
              {label(state.statut) || "Aucun statut à cette date"} ·{" "}
              {state.equipe || "Sans équipe"} ·{" "}
              {state.roles.map((x) => label(x.type)).join(", ") || "Aucun rôle"}
            </p>
          )}
        </section>
      )}
    </Page>
  );
}

function Catalog({ kind }) {
  const { me, has } = useContext(Auth);
  const event = kind === "evenements";
  const [q, setQ] = useState(""),
    [type, setType] = useState(""),
    [year, setYear] = useState(""),
    [team, setTeam] = useState(""),
    [author, setAuthor] = useState(""),
    [form, setForm] = useState(null),
    [detail, setDetail] = useState(null);
  const r = useData(
    `/api/${me ? "" : "public/"}${kind}?q=${encodeURIComponent(q)}${type ? "&type=" + type : ""}${year ? "&annee=" + year : ""}${team ? "&equipeId=" + team : ""}${author ? "&auteurId=" + author : ""}`,
  );
  const members = useData("/api/public/membres?size=100"),
    teams = useData("/api/public/equipes");
  const allowed = event
    ? has("PERMANENT")
    : me && (has("PERMANENT") || has("DOCTORANT") || has("ASSOCIE"));
  const types = event
    ? ["CONFERENCE", "SEMINAIRE", "WORKSHOP"]
    : [
        "ARTICLE_JOURNAL",
        "ARTICLE_CONFERENCE",
        "CHAPITRE_LIVRE",
        "THESE",
        "RAPPORT_TECHNIQUE",
        "AUTRE",
      ];
  const fields = event
    ? [
        field("titre", "Titre"),
        field("typeEvenement", "Type", "select", true, types),
        field("description", "Description", "textarea", false),
        field("edition", "Édition", "text", false),
        field("dateDebut", "Date de début", "date"),
        field("dateFin", "Date de fin", "date", false),
        field("lieu", "Lieu"),
        field(
          "ids",
          "Organisateurs : identifiants séparés par des virgules",
          "text",
          false,
        ),
      ]
    : [
        field("titre", "Titre"),
        field("typePublication", "Type", "select", true, types),
        field("annee", "Année", "number"),
        field("doi", "DOI", "text", false),
        field("lien", "Lien", "url", false),
        field("equipeId", "Équipe", "select", false, opts(teams.data)),
        field(
          "ids",
          "Auteurs dans l’ordre : identifiants séparés par des virgules",
        ),
      ];
  return (
    <Page
      title={event ? "Événements & rencontres" : "Publications scientifiques"}
      description={
        event
          ? "Partager, échanger et faire émerger de nouvelles idées."
          : "Les travaux et les contributions de nos chercheurs."
      }
      actions={
        allowed && (
          <button
            onClick={() =>
              setForm({
                annee: new Date().getFullYear(),
                dateDebut: today(),
                ids: String(me.profil.membreId),
              })
            }
          >
            + {event ? "Nouvel événement" : "Ajouter une publication"}
          </button>
        )
      }
    >
      <div className="filters">
        <input
          placeholder="Rechercher un titre…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">Tous les types</option>
          {types.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
        {!event && (
          <>
            <input
              type="number"
              placeholder="Année"
              value={year}
              onChange={(e) => setYear(e.target.value)}
            />
            <select value={team} onChange={(e) => setTeam(e.target.value)}>
              <option value="">Toutes les équipes</option>
              {teams.data?.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nom}
                </option>
              ))}
            </select>
            <select value={author} onChange={(e) => setAuthor(e.target.value)}>
              <option value="">Tous les auteurs</option>
              {members.data?.content.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.prenom} {m.nom}
                </option>
              ))}
            </select>
          </>
        )}
      </div>
      <Status {...r} />
      {form && (
        <>
          <Form
            key={form.id || "new"}
            fields={fields}
            initial={form}
            onCancel={() => setForm(null)}
            onSubmit={async (v) => {
              const ids = (v.ids || "")
                .split(",")
                .map((x) => Number(x.trim()))
                .filter(Boolean);
              const body = event
                ? { ...v, dateFin: v.dateFin || null, organisateurIds: ids }
                : {
                    ...v,
                    annee: Number(v.annee),
                    equipeId: v.equipeId ? Number(v.equipeId) : null,
                    auteurIds: ids,
                  };
              await api("/api/" + kind + (form.id ? "/" + form.id : ""), {
                method: form.id ? "PUT" : "POST",
                body,
              });
              setForm(null);
              setDetail(null);
              r.reload();
            }}
          />
          <p className="hint">
            Référence des membres :{" "}
            {members.data?.content
              .map((m) => `${m.id} : ${m.prenom} ${m.nom}`)
              .join(" · ")}
          </p>
        </>
      )}
      <Table
        rows={r.data}
        columns={
          event
            ? [
                { key: "titre", label: "Titre" },
                {
                  key: "typeEvenement",
                  label: "Type",
                  render: (r) => <Badge>{r.typeEvenement}</Badge>,
                },
                { key: "edition", label: "Édition" },
                { key: "dateDebut", label: "Date" },
                { key: "lieu", label: "Lieu" },
              ]
            : [
                { key: "titre", label: "Publication" },
                {
                  key: "typePublication",
                  label: "Type",
                  render: (r) => <Badge>{r.typePublication}</Badge>,
                },
                { key: "annee", label: "Année" },
                {
                  key: "auteurs",
                  label: "Auteurs",
                  render: (r) => r.auteurs.map((a) => a.nom).join(", "),
                },
              ]
        }
        actions={(row) => (
          <>
            <button className="secondary" onClick={() => setDetail(row)}>
              Détail
            </button>
            {allowed &&
              (has("DIRECTEUR") ||
                row.createurId === me.profil.membreId ||
                (!event &&
                  row.auteurs?.some(
                    (a) => a.membreId === me.profil.membreId,
                  ))) && (
                <button
                  className="secondary"
                  onClick={() =>
                    setForm({
                      ...row,
                      ids: (event
                        ? row.organisateurIds
                        : row.auteurs.map((a) => a.membreId)
                      ).join(","),
                    })
                  }
                >
                  Modifier
                </button>
              )}
          </>
        )}
      />
      {detail && (
        <section className="panel card">
          <h2>{detail.titre}</h2>
          <p>{detail.description}</p>
          <p>
            {event
              ? `${detail.dateDebut} · ${detail.lieu}`
              : detail.auteurs.map((a) => a.nom).join(", ")}
          </p>
          {detail.doi && <p>DOI : {detail.doi}</p>}
          {detail.lien && /^https?:\/\//.test(detail.lien) && (
            <a href={detail.lien} target="_blank" rel="noreferrer">
              Consulter la publication ↗
            </a>
          )}
          <button className="secondary" onClick={() => setDetail(null)}>
            Fermer
          </button>
        </section>
      )}
    </Page>
  );
}
function Calendar() {
  const [month, setMonth] = useState(today().slice(0, 7));
  const start = month + "-01",
    last = new Date(
      Number(month.slice(0, 4)),
      Number(month.slice(5)),
      0,
    ).getDate();
  const r = useData(`/api/calendrier?debut=${start}&fin=${month}-${last}`);
  const offset = (new Date(start + "T12:00:00").getDay() + 6) % 7;
  return (
    <Page
      title="Calendrier du laboratoire"
      description="Les rendez-vous scientifiques en un coup d’œil."
    >
      <div className="filters">
        <input
          aria-label="Mois"
          type="month"
          value={month}
          onChange={(e) => e.target.value && setMonth(e.target.value)}
        />
      </div>
      <Status {...r} />
      <div className="calendar panel">
        {["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"].map((d) => (
          <div className="day-name" key={d}>
            {d}
          </div>
        ))}
        {Array.from({ length: offset }, (_, i) => (
          <div key={"blank" + i} />
        ))}
        {Array.from({ length: last }, (_, i) => {
          const date = month + "-" + String(i + 1).padStart(2, "0");
          return (
            <div className={"day " + (date === today() ? "today" : "")} key={i}>
              <strong>{i + 1}</strong>
              {r.data
                ?.filter(
                  (e) =>
                    e.dateDebut <= date && (e.dateFin || e.dateDebut) >= date,
                )
                .map((e) => (
                  <div className="calendar-event" key={e.id}>
                    <b>{e.titre}</b>
                    <small>
                      {label(e.typeEvenement)} · {e.lieu}
                    </small>
                  </div>
                ))}
            </div>
          );
        })}
      </div>
    </Page>
  );
}

function Upload({
  onUpload,
  types = [
    "DEMANDE_FINANCEMENT",
    "PROGRAMME",
    "ATTESTATION",
    "RAPPORT",
    "ADMINISTRATIF",
    "PV",
    "CV",
    "AUTRE",
  ],
  parentId,
}) {
  const [file, setFile] = useState(null),
    [type, setType] = useState(types[0]),
    [event, setEvent] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const events = useData("/api/evenements");
  return (
    <form
      className="panel card"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError("");
        try {
          const body = new FormData();
          body.append("file", file);
          body.append("type", type);
          if (event) body.append("evenementId", event);
          if (parentId) body.append("parentId", parentId);
          await onUpload(body);
          setFile(null);
          e.target.reset();
        } catch (e) {
          setError(e.message);
        } finally {
          setBusy(false);
        }
      }}
    >
      <h2>
        {parentId
          ? "Nouvelle version du document #" + parentId
          : "Déposer un document"}
      </h2>
      <div className="filters">
        <input
          type="file"
          required
          accept="application/pdf,image/png,image/jpeg"
          onChange={(e) => setFile(e.target.files[0])}
        />
        <select value={type} onChange={(e) => setType(e.target.value)}>
          {types.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
        <select value={event} onChange={(e) => setEvent(e.target.value)}>
          <option value="">Sans événement</option>
          {events.data?.map((e) => (
            <option key={e.id} value={e.id}>
              {e.titre}
            </option>
          ))}
        </select>
        <button disabled={busy}>Déposer</button>
      </div>
      <small>PDF, PNG ou JPEG · 20 Mo maximum</small>
      {error && <p className="alert">{error}</p>}
    </form>
  );
}
function Documents() {
  const [q, setQ] = useState(""),
    [type, setType] = useState(""),
    [event, setEvent] = useState(""),
    [date, setDate] = useState(""),
    [version, setVersion] = useState(null),
    [message, setMessage] = useState("");
  const r = useData(
    `/api/documents?q=${encodeURIComponent(q)}${type ? "&type=" + type : ""}${event ? "&evenementId=" + event : ""}${date ? "&date=" + date : ""}`,
  );
  const events = useData("/api/evenements");
  return (
    <Page
      title="Bibliothèque documentaire"
      description="Documents, archives et versions du laboratoire."
    >
      <Upload
        key={version || "new"}
        parentId={version}
        onUpload={async (body) => {
          await api("/api/documents", { method: "POST", body });
          r.reload();
          setVersion(null);
          setMessage("Document archivé.");
        }}
      />
      {message && <p className="success">{message}</p>}
      <div className="filters">
        <input
          placeholder="Rechercher un fichier…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">Tous les types</option>
          {[
            "DEMANDE_FINANCEMENT",
            "PROGRAMME",
            "ATTESTATION",
            "RAPPORT",
            "ADMINISTRATIF",
            "PV",
            "CV",
            "AUTRE",
          ].map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
        <select value={event} onChange={(e) => setEvent(e.target.value)}>
          <option value="">Tous les événements</option>
          {events.data?.map((e) => (
            <option key={e.id} value={e.id}>
              {e.titre}
            </option>
          ))}
        </select>
        <input
          type="date"
          aria-label="Date du dépôt"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
      </div>
      <Status {...r} />
      <Table
        rows={r.data}
        columns={[
          { key: "nomFichier", label: "Document" },
          { key: "typeDocument", label: "Type" },
          { key: "version", label: "Version" },
          { key: "uploadedBy", label: "Déposé par" },
          {
            key: "dateUpload",
            label: "Date",
            render: (r) => r.dateUpload?.slice(0, 10),
          },
        ]}
        actions={(d) => (
          <>
            <button
              className="secondary"
              onClick={() =>
                download(d.id, d.nomFichier).catch((e) => setMessage(e.message))
              }
            >
              Télécharger
            </button>
            <button
              className="secondary"
              onClick={() => {
                setVersion(d.id);
                window.scrollTo({ top: 0, behavior: "smooth" });
              }}
            >
              Nouvelle version
            </button>
          </>
        )}
      />
    </Page>
  );
}
function Meetings() {
  const { me, has } = useContext(Auth);
  const r = useData("/api/reunions");
  const [form, setForm] = useState(false),
    [error, setError] = useState("");
  return (
    <Page
      title="Réunions & procès-verbaux"
      actions={
        <button onClick={() => setForm(!form)}>+ Organiser une réunion</button>
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {form && (
        <Form
          initial={{ dateReunion: today() }}
          fields={[
            field("titre", "Titre"),
            field("dateReunion", "Date", "date"),
            field("ordreDuJour", "Ordre du jour", "textarea"),
          ]}
          onCancel={() => setForm(false)}
          onSubmit={async (v) => {
            await api("/api/reunions", { method: "POST", body: v });
            setForm(false);
            r.reload();
          }}
        />
      )}
      <Table
        rows={r.data}
        columns={[
          { key: "titre", label: "Réunion" },
          { key: "dateReunion", label: "Date" },
          { key: "ordreDuJour", label: "Ordre du jour" },
        ]}
        actions={(r) => (
          <>
            {r.pvDocumentId && (
              <button
                className="secondary"
                onClick={() =>
                  download(r.pvDocumentId, "PV.pdf").catch((e) =>
                    setError(e.message),
                  )
                }
              >
                Télécharger PV
              </button>
            )}
            {(has("DIRECTEUR") || r.createurId === me.profil.membreId) && (
              <label className="upload-button">
                Déposer PV
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={async (e) => {
                    if (!e.target.files[0]) return;
                    try {
                      const body = new FormData();
                      body.append("file", e.target.files[0]);
                      await api(`/api/reunions/${r.id}/pv`, {
                        method: "PUT",
                        body,
                      });
                      window.dispatchEvent(new Event("meeting-reload"));
                    } catch (e) {
                      setError(e.message);
                    }
                  }}
                />
              </label>
            )}
          </>
        )}
      />
      <ReloadEvent name="meeting-reload" reload={r.reload} />
    </Page>
  );
}
function ReloadEvent({ name, reload }) {
  useEffect(() => {
    window.addEventListener(name, reload);
    return () => window.removeEventListener(name, reload);
  }, [name, reload]);
  return null;
}
function Needs() {
  const { has } = useContext(Auth);
  const r = useData("/api/besoins");
  const [form, setForm] = useState(false),
    [error, setError] = useState("");
  async function decide(id, statut) {
    if (!confirm("Confirmer cette décision ?")) return;
    try {
      await api(`/api/besoins/${id}/decision`, {
        method: "PATCH",
        body: { statut },
      });
      r.reload();
    } catch (e) {
      setError(e.message);
    }
  }
  return (
    <Page
      title="Besoins matériels"
      description="Exprimer un besoin et suivre sa décision."
      actions={
        <button onClick={() => setForm(!form)}>+ Exprimer un besoin</button>
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {form && (
        <Form
          fields={[
            field("designation", "Désignation"),
            { ...field("quantiteDemandee", "Quantité", "number"), min: 1 },
            field("justification", "Justification", "textarea"),
          ]}
          onCancel={() => setForm(false)}
          onSubmit={async (v) => {
            await api("/api/besoins", {
              method: "POST",
              body: { ...v, quantiteDemandee: Number(v.quantiteDemandee) },
            });
            setForm(false);
            r.reload();
          }}
        />
      )}
      <Table
        rows={r.data}
        columns={[
          { key: "designation", label: "Besoin" },
          { key: "demandeur", label: "Demandeur" },
          { key: "quantiteDemandee", label: "Quantité" },
          { key: "justification", label: "Justification" },
          {
            key: "statutBesoin",
            label: "État",
            render: (r) => <Badge>{r.statutBesoin}</Badge>,
          },
        ]}
        actions={
          has("DIRECTEUR")
            ? (b) =>
                b.statutBesoin === "EN_ATTENTE" && (
                  <>
                    <button onClick={() => decide(b.id, "VALIDE")}>
                      Valider
                    </button>
                    <button
                      className="danger"
                      onClick={() => decide(b.id, "REFUSE")}
                    >
                      Refuser
                    </button>
                  </>
                )
            : null
        }
      />
    </Page>
  );
}
function Material() {
  const { has } = useContext(Auth);
  const r = useData("/api/materiels");
  const ms = useData("/api/public/membres?size=100");
  const [form, setForm] = useState(""),
    [equity, setEquity] = useState(null),
    [error, setError] = useState("");
  return (
    <Page
      title="Inventaire & distribution"
      description="Un stock suivi et des attributions traçables."
      actions={
        has("DIRECTEUR") && (
          <div className="row-actions">
            <button onClick={() => setForm("arrive")}>+ Arrivage</button>
            <button
              className="secondary"
              onClick={() => setForm("distribution")}
            >
              Distribuer
            </button>
            <button
              className="secondary"
              onClick={async () => {
                try {
                  setEquity(await api("/api/materiels/equite"));
                } catch (e) {
                  setError(e.message);
                }
              }}
            >
              Vue d’équité
            </button>
          </div>
        )
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {form === "arrive" && (
        <Form
          initial={{ dateArrivage: today() }}
          fields={[
            field("designation", "Désignation"),
            field("description", "Description", "textarea", false),
            {
              ...field("quantiteInitiale", "Quantité initiale", "number"),
              min: 1,
            },
            field("dateArrivage", "Date d’arrivage", "date"),
          ]}
          onCancel={() => setForm("")}
          onSubmit={async (v) => {
            await api("/api/materiels", {
              method: "POST",
              body: { ...v, quantiteInitiale: Number(v.quantiteInitiale) },
            });
            setForm("");
            r.reload();
          }}
        />
      )}
      {form === "distribution" && (
        <Form
          initial={{ dateDistribution: today() }}
          fields={[
            field("materielId", "Matériel", "select", true, opts(r.data)),
            field(
              "membreId",
              "Bénéficiaire",
              "select",
              true,
              ms.data?.content.map((m) => ({
                value: m.id,
                label: m.prenom + " " + m.nom,
              })),
            ),
            { ...field("quantite", "Quantité", "number"), min: 1 },
            field("dateDistribution", "Date", "date"),
          ]}
          onCancel={() => setForm("")}
          onSubmit={async (v) => {
            await api("/api/distributions", {
              method: "POST",
              body: {
                ...v,
                materielId: Number(v.materielId),
                membreId: Number(v.membreId),
                quantite: Number(v.quantite),
              },
            });
            setForm("");
            setEquity(null);
            r.reload();
          }}
        />
      )}
      <Table
        rows={r.data}
        columns={[
          { key: "designation", label: "Matériel" },
          { key: "description", label: "Description" },
          { key: "quantiteInitiale", label: "Quantité reçue" },
          { key: "quantiteDisponible", label: "Disponible" },
          { key: "dateArrivage", label: "Arrivage" },
        ]}
      />
      {equity && (
        <>
          <h2>Répartition · aide à la décision</h2>
          <Table
            rows={equity}
            columns={[
              { key: "membre", label: "Permanent" },
              { key: "quantiteTotale", label: "Quantité reçue" },
              {
                key: "distributions",
                label: "Détail",
                render: (r) =>
                  r.distributions
                    .map((d) => `${d.materiel} × ${d.quantite} (${d.date})`)
                    .join(", ") || "Aucune attribution",
              },
            ]}
          />
        </>
      )}
    </Page>
  );
}
function Partners() {
  const { has } = useContext(Auth);
  const r = useData("/api/partenaires");
  const [form, setForm] = useState(null);
  return (
    <Page
      title="Partenaires"
      description="Des liens durables avec le monde académique et institutionnel."
      actions={
        has("DIRECTEUR") && (
          <button onClick={() => setForm({})}>+ Nouveau partenaire</button>
        )
      }
    >
      <Status {...r} />
      {form && (
        <Form
          key={form.id || "new"}
          initial={form}
          fields={[
            field("nom", "Nom"),
            field("typePartenaire", "Type", "select", true, [
              "ACADEMIQUE",
              "INDUSTRIEL",
              "INSTITUTIONNEL",
            ]),
            field("contactEmail", "E-mail de contact", "email", false),
            field("pays", "Pays"),
            field("description", "Description", "textarea", false),
          ]}
          onCancel={() => setForm(null)}
          onSubmit={async (v) => {
            await api("/api/partenaires" + (form.id ? "/" + form.id : ""), {
              method: form.id ? "PUT" : "POST",
              body: v,
            });
            setForm(null);
            r.reload();
          }}
        />
      )}
      <Table
        rows={r.data}
        columns={[
          { key: "nom", label: "Partenaire" },
          { key: "typePartenaire", label: "Type" },
          { key: "contactEmail", label: "Contact" },
          { key: "pays", label: "Pays" },
          { key: "description", label: "Description" },
        ]}
        actions={
          has("DIRECTEUR")
            ? (p) => (
                <button className="secondary" onClick={() => setForm(p)}>
                  Modifier
                </button>
              )
            : null
        }
      />
    </Page>
  );
}
function Agreements() {
  const { has } = useContext(Auth);
  const r = useData("/api/conventions"),
    p = useData("/api/partenaires"),
    docs = useData("/api/documents");
  const [form, setForm] = useState(null),
    [error, setError] = useState("");
  return (
    <Page
      title="Conventions"
      description="Suivi des accords et de leurs archives documentaires."
      actions={
        has("DIRECTEUR") && (
          <button
            onClick={() =>
              setForm({ dateDebut: today(), statutConvention: "ACTIVE" })
            }
          >
            + Nouvelle convention
          </button>
        )
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {form && (
        <Form
          key={form.id || "new"}
          initial={form}
          fields={[
            field("titre", "Titre"),
            field("partenaireId", "Partenaire", "select", true, opts(p.data)),
            field("dateDebut", "Date de début", "date"),
            field("dateFin", "Date de fin", "date", false),
            field("statutConvention", "Statut", "select", true, [
              "ACTIVE",
              "EXPIREE",
              "RESILIEE",
            ]),
            field(
              "documentId",
              "Archive documentaire",
              "select",
              false,
              docs.data?.map((d) => ({ value: d.id, label: d.nomFichier })),
            ),
          ]}
          onCancel={() => setForm(null)}
          onSubmit={async (v) => {
            await api("/api/conventions" + (form.id ? "/" + form.id : ""), {
              method: form.id ? "PUT" : "POST",
              body: {
                ...v,
                partenaireId: Number(v.partenaireId),
                documentId: v.documentId ? Number(v.documentId) : null,
                dateFin: v.dateFin || null,
              },
            });
            setForm(null);
            r.reload();
          }}
        />
      )}
      <p className="hint">
        Déposez le document signé dans la bibliothèque, puis associez-le à la
        convention.
      </p>
      <Table
        rows={r.data}
        columns={[
          { key: "titre", label: "Convention" },
          { key: "partenaire", label: "Partenaire" },
          { key: "dateDebut", label: "Début" },
          { key: "dateFin", label: "Fin" },
          {
            key: "statutConvention",
            label: "État",
            render: (r) => <Badge>{r.statutConvention}</Badge>,
          },
        ]}
        actions={(c) => (
          <>
            {c.documentId && (
              <button
                className="secondary"
                onClick={() =>
                  download(c.documentId, "Convention.pdf").catch((e) =>
                    setError(e.message),
                  )
                }
              >
                Document
              </button>
            )}
            {has("DIRECTEUR") && (
              <button className="secondary" onClick={() => setForm(c)}>
                Modifier
              </button>
            )}
          </>
        )}
      />
    </Page>
  );
}
function Reports() {
  const { has } = useContext(Auth);
  const r = useData("/api/rapports");
  const [year, setYear] = useState(new Date().getFullYear()),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Page
      title="Rapports annuels"
      description="Bilan scientifique, membres, gouvernance et activités du laboratoire."
    >
      {has("DIRECTEUR") && (
        <div className="panel card">
          <h2>Générer un bilan PDF</h2>
          <p>
            Une nouvelle génération archive une nouvelle version du rapport de
            l’année.
          </p>
          <div className="filters">
            <input
              aria-label="Année"
              type="number"
              min="1900"
              max={new Date().getFullYear()}
              value={year}
              onChange={(e) => setYear(e.target.value)}
            />
            <button
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                setError("");
                try {
                  await api(`/api/rapports/${year}/generer`, {
                    method: "POST",
                  });
                  r.reload();
                } catch (e) {
                  setError(e.message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? "Génération…" : "Générer le rapport"}
            </button>
          </div>
        </div>
      )}
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      <Table
        rows={r.data}
        columns={[
          { key: "annee", label: "Année" },
          {
            key: "dateGeneration",
            label: "Dernière génération",
            render: (r) => new Date(r.dateGeneration).toLocaleString("fr-FR"),
          },
        ]}
        actions={(r) => (
          <button
            className="secondary"
            onClick={() =>
              download(r.documentId, `Rapport-LIAS-${r.annee}.pdf`).catch((e) =>
                setError(e.message),
              )
            }
          >
            Télécharger PDF
          </button>
        )}
      />
    </Page>
  );
}
function Admissions() {
  const r = useData("/api/adhesions");
  const teams = useData("/api/equipes");
  const [form, setForm] = useState(null);
  const [invitation, setInvitation] = useState(null);
  const [copyMessage, setCopyMessage] = useState("");
  return (
    <Page
      title="Demandes d’adhésion"
      description="Examiner les candidatures et accueillir les futurs membres."
    >
      <Status {...r} />
      {invitation && (
        <section
          className="panel card"
          aria-label="Invitation du nouveau membre"
        >
          <h2>Compte créé : transmettre le lien d’activation</h2>
          <p>
            <strong>{invitation.email}</strong> peut choisir son mot de passe
            avec ce lien. Valable jusqu’au{" "}
            {new Date(invitation.activationExpiration).toLocaleString("fr-FR")}.
          </p>
          <label>
            Lien d’activation
            <input
              style={{ width: "100%" }}
              readOnly
              value={invitation.activationUrl}
              onFocus={(e) => e.target.select()}
            />
          </label>
          <div className="row-actions" style={{ marginTop: 16 }}>
            <button
              type="button"
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(invitation.activationUrl);
                  setCopyMessage("Lien copié.");
                } catch {
                  setCopyMessage(
                    "Sélectionnez le lien ci-dessus et utilisez Ctrl+C.",
                  );
                }
              }}
            >
              Copier le lien
            </button>
            <a
              className="button secondary"
              href={invitation.activationUrl}
              target="_blank"
              rel="noreferrer"
            >
              Ouvrir la page d’activation
            </a>
          </div>
          <p role="status">{copyMessage}</p>
          <small>
            Invitation personnelle : remettre uniquement au candidat. Aucun mot
            de passe n’est défini par le directeur.
          </small>
        </section>
      )}
      {form && (
        <section>
          <h2>
            {form.prenom} {form.nom} · {form.email}
          </h2>
          <p className="panel card">
            {form.motivation}
            {form.cvDocumentId && (
              <button
                className="secondary"
                onClick={() =>
                  download(form.cvDocumentId, "CV.pdf").catch((e) =>
                    alert(e.message),
                  )
                }
              >
                Télécharger le CV
              </button>
            )}
          </p>
          <Form
            key={form.id}
            initial={{ accepter: "true", statut: "DOCTORANT" }}
            fields={[
              field("accepter", "Décision", "select", true, [
                { value: "true", label: "Accepter" },
                { value: "false", label: "Refuser" },
              ]),
              field("statut", "Statut initial", "select", true, [
                "PERMANENT",
                "DOCTORANT",
                "ASSOCIE",
              ]),
              field("equipeId", "Équipe", "select", false, opts(teams.data)),
              field(
                "motif",
                "Motif (obligatoire pour un refus)",
                "textarea",
                false,
              ),
            ]}
            onCancel={() => setForm(null)}
            submit="Confirmer la décision"
            onSubmit={async (v) => {
              const decision = await api(`/api/adhesions/${form.id}/decision`, {
                method: "POST",
                body: {
                  ...v,
                  accepter: v.accepter === "true",
                  equipeId: v.equipeId ? Number(v.equipeId) : null,
                },
              });
              setInvitation(decision.activationUrl ? decision : null);
              setCopyMessage("");
              setForm(null);
              r.reload();
            }}
          />
        </section>
      )}
      <Table
        rows={r.data}
        columns={[
          {
            key: "nom",
            label: "Candidat",
            render: (r) => r.prenom + " " + r.nom,
          },
          { key: "email", label: "E-mail" },
          {
            key: "dateSoumission",
            label: "Soumission",
            render: (r) => r.dateSoumission.slice(0, 10),
          },
          {
            key: "statutDemande",
            label: "État",
            render: (r) => <Badge>{r.statutDemande}</Badge>,
          },
          { key: "motifRefus", label: "Motif" },
        ]}
        actions={(d) => (
          <>
            {d.statutDemande === "EN_ATTENTE" && (
              <button onClick={() => setForm(d)}>Examiner</button>
            )}
            {d.activationUrl && (
              <button
                className="secondary"
                onClick={() => {
                  setInvitation(d);
                  setCopyMessage("");
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              >
                Lien d’activation
              </button>
            )}
          </>
        )}
      />
    </Page>
  );
}
function Governance() {
  const { has } = useContext(Auth);
  const members = useData("/api/membres?size=100"),
    teams = useData("/api/equipes"),
    mandates = useData("/api/mandats");
  const [id, setId] = useState(""),
    [action, setAction] = useState(""),
    [periods, setPeriods] = useState(null),
    [error, setError] = useState(""),
    [success, setSuccess] = useState("");
  const options = [
    ...(has("ADMIN")
      ? [
          ["statut", "Changer de statut"],
          ["affiliation", "Départ / retour"],
          ["mandat", "Créer un mandat"],
        ]
      : []),
    ["equipe", "Changer d’équipe"],
    ...(has("DIRECTEUR") ? [["roles", "Attribuer un rôle"]] : []),
  ];
  const refresh = async () => {
    if (id) setPeriods(await api(`/api/membres/${id}/periodes`));
    mandates.reload();
  };
  const fields =
    action === "statut"
      ? [
          field("type", "Statut", "select", true, [
            "PERMANENT",
            "ASSOCIE",
            "DOCTORANT",
            "RETRAITE",
            "ANCIEN",
          ]),
          field("dateDebut", "Date du changement", "date"),
        ]
      : action === "affiliation"
        ? [
            field("action", "Action", "select", true, ["DEPART", "RETOUR"]),
            field("type", "Statut en cas de retour", "select", false, [
              "PERMANENT",
              "ASSOCIE",
              "DOCTORANT",
            ]),
            field("date", "Date", "date"),
          ]
        : action === "equipe"
          ? [
              field("equipeId", "Équipe", "select", true, opts(teams.data)),
              field("dateDebut", "Date de début", "date"),
            ]
          : action === "roles"
            ? [
                field("type", "Rôle", "select", true, [
                  "DIRECTEUR",
                  "VICE_DIRECTEUR",
                  "CHEF_EQUIPE",
                  "MEMBRE_EQUIPE",
                ]),
                field(
                  "equipeId",
                  "Équipe (rôles équipe uniquement)",
                  "select",
                  false,
                  opts(teams.data),
                ),
                field(
                  "mandatId",
                  "Mandat",
                  "select",
                  false,
                  opts(mandates.data?.filter((m) => !m.dateFin)),
                ),
                field("dateDebut", "Date de début", "date"),
              ]
            : [
                field("libelle", "Libellé du mandat"),
                field("dateDebut", "Date de début", "date"),
              ];
  return (
    <Page
      title="Gouvernance & historisation"
      description="Clôturer une période et en ouvrir une nouvelle, en conservant toutes les traces."
    >
      <Status busy={members.busy} error={members.error || error} />
      {success && <p className="success">{success}</p>}
      <div className="filters">
        <select
          value={id}
          onChange={async (e) => {
            setId(e.target.value);
            setPeriods(null);
            if (e.target.value)
              try {
                setPeriods(
                  await api(`/api/membres/${e.target.value}/periodes`),
                );
              } catch (e) {
                setError(e.message);
              }
          }}
        >
          <option value="">Choisir un membre</option>
          {members.data?.content.map((m) => (
            <option key={m.id} value={m.id}>
              {m.prenom} {m.nom}
            </option>
          ))}
        </select>
        <select value={action} onChange={(e) => setAction(e.target.value)}>
          <option value="">Choisir une opération</option>
          {options.map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
      </div>
      {action && (id || action === "mandat") && (
        <Form
          key={action + id}
          initial={{ dateDebut: today(), date: today() }}
          fields={fields}
          onCancel={() => setAction("")}
          onSubmit={async (v) => {
            const body = { ...v };
            for (const key of ["equipeId", "mandatId"])
              if (key in body) body[key] = body[key] ? Number(body[key]) : null;
            await api(
              action === "mandat"
                ? "/api/mandats"
                : `/api/membres/${id}/${action}`,
              { method: "POST", body },
            );
            setAction("");
            setSuccess(
              "Opération enregistrée. Les anciennes périodes sont conservées.",
            );
            await refresh();
          }}
        />
      )}
      {periods &&
        Object.entries(periods).map(([name, rows]) => (
          <section key={name}>
            <h2>{label(name)}</h2>
            <Table
              rows={rows}
              columns={[
                {
                  key: "type",
                  label: "Valeur",
                  render: (r) => label(r.type) || r.equipe || "Affiliation",
                },
                { key: "dateDebut", label: "Début" },
                {
                  key: "dateFin",
                  label: "Fin",
                  render: (r) => r.dateFin || <Badge>Période ouverte</Badge>,
                },
              ]}
              actions={
                name === "roles" && has("DIRECTEUR")
                  ? (r) =>
                      !r.dateFin && (
                        <button
                          className="secondary"
                          onClick={async () => {
                            if (
                              !confirm(
                                "Clôturer ce rôle à la date d’aujourd’hui ?",
                              )
                            )
                              return;
                            try {
                              await api(
                                `/api/membres/${id}/roles/${r.id}/cloturer`,
                                { method: "PATCH", body: { dateFin: today() } },
                              );
                              await refresh();
                            } catch (e) {
                              setError(e.message);
                            }
                          }}
                        >
                          Clôturer
                        </button>
                      )
                  : null
              }
            />
          </section>
        ))}
      <h2>Mandats</h2>
      <Table
        rows={mandates.data}
        columns={[
          { key: "libelle", label: "Mandat" },
          { key: "dateDebut", label: "Début" },
          { key: "dateFin", label: "Fin", render: (r) => r.dateFin || "Actif" },
        ]}
        actions={
          has("ADMIN")
            ? (m) =>
                !m.dateFin && (
                  <button
                    className="danger"
                    onClick={async () => {
                      if (
                        !confirm(
                          "Clôturer le mandat et les rôles associés ? Le directeur perdra immédiatement ses droits.",
                        )
                      )
                        return;
                      try {
                        await api(`/api/mandats/${m.id}/cloturer`, {
                          method: "PATCH",
                          body: { dateFin: today() },
                        });
                        mandates.reload();
                      } catch (e) {
                        setError(e.message);
                      }
                    }}
                  >
                    Clôturer
                  </button>
                )
            : null
        }
      />
    </Page>
  );
}
function Accounts() {
  const r = useData("/api/admin/comptes");
  const [error, setError] = useState("");
  return (
    <Page
      title="Administration des comptes"
      description="États techniques des comptes, indépendants des statuts et rôles métier."
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      <Table
        rows={r.data}
        columns={[
          { key: "email", label: "Compte" },
          {
            key: "etatCompte",
            label: "État",
            render: (r) => <Badge>{r.etatCompte}</Badge>,
          },
          {
            key: "estAdmin",
            label: "Administrateur",
            render: (r) => (r.estAdmin ? "Oui" : "Non"),
          },
          {
            key: "derniereConnexion",
            label: "Dernière connexion",
            render: (r) =>
              r.derniereConnexion
                ? new Date(r.derniereConnexion).toLocaleString("fr-FR")
                : "—",
          },
        ]}
        actions={(u) => (
          <select
            aria-label={"État de " + u.email}
            value={u.etatCompte}
            onChange={async (e) => {
              const etat = e.target.value;
              if (!confirm(`Passer ${u.email} à l’état ${etat} ?`)) return;
              try {
                await api(`/api/admin/comptes/${u.id}/etat`, {
                  method: "PATCH",
                  body: { etat },
                });
                r.reload();
              } catch (e) {
                setError(e.message);
              }
            }}
          >
            {["ACTIF", "GELE", "DESACTIVE"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        )}
      />
    </Page>
  );
}
function Discussions() {
  const r = useData("/api/discussions");
  const teams = useData("/api/equipes"),
    events = useData("/api/evenements");
  const [form, setForm] = useState(false),
    [id, setId] = useState(null);
  return (
    <Page
      title="Discussions"
      description="Un espace d’échange pour les équipes et la vie du laboratoire."
      actions={
        <button onClick={() => setForm(!form)}>+ Nouvelle discussion</button>
      }
    >
      <Status {...r} />
      {form && (
        <Form
          fields={[
            field("titre", "Titre"),
            field("typeDiscussion", "Type", "select", true, [
              "GENERALE",
              "EQUIPE",
              "EVENEMENT",
            ]),
            field(
              "equipeId",
              "Équipe (si discussion équipe)",
              "select",
              false,
              opts(teams.data),
            ),
            field(
              "evenementId",
              "Événement (si discussion événement)",
              "select",
              false,
              opts(events.data),
            ),
          ]}
          onCancel={() => setForm(false)}
          onSubmit={async (v) => {
            const d = await api("/api/discussions", {
              method: "POST",
              body: {
                ...v,
                equipeId: v.equipeId ? Number(v.equipeId) : null,
                evenementId: v.evenementId ? Number(v.evenementId) : null,
              },
            });
            setForm(false);
            r.reload();
            setId(d.id);
          }}
        />
      )}
      <div className="discussion-grid">
        <div>
          {r.data?.map((d) => (
            <button
              key={d.id}
              className={"thread panel " + (id === d.id ? "selected" : "")}
              onClick={() => setId(d.id)}
            >
              <strong>{d.titre}</strong>
              <Badge>{d.typeDiscussion}</Badge>
            </button>
          ))}
        </div>
        {id ? (
          <Messages key={id} id={id} />
        ) : (
          <div className="empty panel">
            Sélectionnez une discussion pour consulter les messages.
          </div>
        )}
      </div>
    </Page>
  );
}
function Messages({ id }) {
  const r = useData(`/api/discussions/${id}/messages`);
  useEffect(() => {
    const t = setInterval(r.reload, 15000);
    return () => clearInterval(t);
  }, [id]);
  return (
    <section className="panel card">
      <h2>Échanges</h2>
      <Status {...r} />
      <div className="messages">
        {r.data?.map((m) => (
          <article className="message" key={m.id}>
            <strong>{m.auteur}</strong>
            <small>{new Date(m.dateEnvoi).toLocaleString("fr-FR")}</small>
            <p>{m.contenu}</p>
          </article>
        ))}
      </div>
      <Form
        fields={[field("contenu", "Votre message", "textarea")]}
        submit="Envoyer"
        onSubmit={async (v) => {
          await api(`/api/discussions/${id}/messages`, {
            method: "POST",
            body: v,
          });
          r.reload();
        }}
      />
    </section>
  );
}
function Notifications() {
  const r = useData("/api/notifications");
  const [error, setError] = useState("");
  return (
    <Page
      title="Notifications"
      description="Les dernières nouvelles et décisions du laboratoire."
      actions={
        <button
          onClick={async () => {
            try {
              await api("/api/notifications/tout-lire", { method: "PATCH" });
              r.reload();
            } catch (e) {
              setError(e.message);
            }
          }}
        >
          Tout marquer comme lu
        </button>
      }
    >
      <Status {...r} />
      {error && <p className="alert">{error}</p>}
      {r.data?.map((n) => (
        <article
          className={"panel notification " + (!n.lu ? "unread" : "")}
          key={n.id}
        >
          <div>
            <Badge>{n.type}</Badge>
            <h2>{n.titre}</h2>
            <p>{n.contenu}</p>
            <small>{new Date(n.dateCreation).toLocaleString("fr-FR")}</small>
          </div>
          {!n.lu && (
            <button
              className="secondary"
              onClick={async () => {
                try {
                  await api(`/api/notifications/${n.id}/lire`, {
                    method: "PATCH",
                  });
                  r.reload();
                } catch (e) {
                  setError(e.message);
                }
              }}
            >
              Marquer lu
            </button>
          )}
        </article>
      ))}
    </Page>
  );
}
function Search() {
  const [q, setQ] = useState(""),
    [query, setQuery] = useState("");
  return (
    <Page
      title="Recherche globale"
      description="Membres, événements, publications et documents accessibles."
    >
      <form
        className="filters"
        onSubmit={(e) => {
          e.preventDefault();
          setQuery(q);
        }}
      >
        <input
          aria-label="Recherche"
          required
          minLength="2"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Que recherchez-vous ?"
        />
        <button>Rechercher</button>
      </form>
      {query && <SearchResults key={query} q={query} />}
    </Page>
  );
}
function SearchResults({ q }) {
  const r = useData("/api/recherche?q=" + encodeURIComponent(q));
  const paths = {
    MEMBRE: "membres",
    EVENEMENT: "evenements",
    PUBLICATION: "publications",
    DOCUMENT: "documents",
  };
  return (
    <>
      <Status {...r} />
      <Table
        rows={r.data}
        columns={[
          {
            key: "type",
            label: "Type",
            render: (r) => <Badge>{r.type}</Badge>,
          },
          {
            key: "titre",
            label: "Résultat",
            render: (r) => (
              <Link
                to={
                  "/" + paths[r.type] + (r.type === "MEMBRE" ? "/" + r.id : "")
                }
              >
                {r.titre}
              </Link>
            ),
          },
          { key: "description", label: "Description" },
        ]}
      />
    </>
  );
}

createRoot(document.getElementById("root")).render(<App />);
