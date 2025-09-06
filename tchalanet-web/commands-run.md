Create project:
npx create-nx-workspace@latest tchalanet-web \
--preset=angular-monorepo \
--appName=web \
--style=scss \
--routing=true

✔ esbuild
✔ NO ssr for now
✔  vitest
✔  playwright
✔  github actions


-- Change config initial
---nx.json
"nameAndDirectoryFormat": "as-provided",
"type": "component",        // => *.component.ts
"flat": true,               // fichiers plats
"inlineTemplate": true,     // HTML inline par défaut
"inlineStyle": true

---project.json
"prefix": "tch"
