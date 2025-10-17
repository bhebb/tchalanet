#!/usr/bin/env bash
# ============================================================
# Aliases Nx pour accélérer le dev Angular/Ionic avec Nx 21.4.1
# ============================================================

# --- LIBS ---------------------------------------------------

# Générer une lib Angular standalone
alias nxlib='nx g @nx/angular:lib --standalone --prefix=tchl'

# Générer une lib utilitaire TypeScript (pas Angular)
alias nxlibts='nx g @nx/js:lib --bundler=tsc --unit-test-runner=jest'

# Générer une lib Angular Material
alias nxlibmat='nx g @nx/angular:lib --standalone --prefix=tchl --style=scss --directory=libs/ui/material'

# --- COMPONENTS --------------------------------------------

# Générer un component standalone avec OnPush
alias nxc='nx g @nx/angular:component --standalone --change-detection=OnPush --export --prefix=tchl'

# Générer un widget (dans libs/ui/widgets)
alias nxcw='nxc libs/ui/widgets'

# Générer une page (component + route)
alias nxpage='nx g @nx/angular:component --standalone --change-detection=OnPush --flat --routing --prefix=tchl'

# --- SERVICES / GUARDS / INTERCEPTORS -----------------------

# Service Angular
alias nxs='nx g @nx/angular:service --prefix=tchl'

# Guard Angular
alias nxg='nx g @nx/angular:guard --prefix=tchl'

# Interceptor Angular
alias nxi='nx g @nx/angular:interceptor --prefix=tchl'

# --- STATE MANAGEMENT ---------------------------------------

# Générer un store NgRx (signal store friendly)
alias nxstore='nx g @nx/angular:ngrx-feature'

# --- TESTS / UTILITAIRES -----------------------------------

# Générer un spec jest pour un fichier existant
alias nxspec='nx g @nx/jest:spec'

# Lancer tests sur tout
alias nxtest='nx run-many --target=test --all'

# --- APPS ---------------------------------------------------

# Générer une nouvelle app Angular
alias nxapp='nx g @nx/angular:app --standalone --routing --style=scss'

# Générer une nouvelle app Ionic Angular
alias nxionic='nx g @nx/angular:app --standalone --routing --style=scss --framework=ionic-angular'

# --- SERVE / BUILD ------------------------------------------

# Serve une app
alias nxserve='nx serve'

# Build une app
alias nxbuild='nx build'

# Linter
alias nxlint='nx run-many --target=lint --all'

# Format
alias nxfmt='nx format:write'
