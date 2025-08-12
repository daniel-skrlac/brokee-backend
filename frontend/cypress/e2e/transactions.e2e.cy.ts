/// <reference types="cypress" />

const NOTE_PREFIX = 'e2e-milk-';
const PLAN_PREFIX = 'e2e-plan-';
const TX_PATH = '/transaction';
const PAGE_SIZE = 10;

Cypress.on('uncaught:exception', (err) => {
    if (String(err && (err.message || err)).includes('Notifications.subscribe')) return false;
    return true;
});

function aliasApi(): void {
    cy.intercept('GET', '**/api/transactions**').as('getTx');
    cy.intercept('POST', '**/api/transactions').as('postTx');
    cy.intercept('PATCH', '**/api/transactions/*').as('patchTx');
    cy.intercept('DELETE', '**/api/transactions/*').as('delTx');
    cy.intercept('GET', '**/api/planned-transactions**').as('getPlanned');
    cy.intercept('POST', '**/api/planned-transactions').as('postPlanned');
    cy.intercept('DELETE', '**/api/planned-transactions/*').as('delPlanned');
    cy.intercept('GET', '**/api/categories').as('getCats');
}

let seq = { note: 0, plan: 0 };

function nextNote(): string {
    seq.note += 1;
    return `${NOTE_PREFIX}test${seq.note}`;
}

function nextPlanTitle(): string {
    seq.plan += 1;
    return `${PLAN_PREFIX}test${seq.plan}`;
}

function requireEnv(key: string): string {
    const v = Cypress.env(key);
    if (!v) throw new Error(`Missing env key: ${key}`);
    return String(v);
}

function ensureListView(): void {
    cy.then(() => {
        const $seg = Cypress.$('.segmented button');
        if ($seg.length) {
            const isActive = Cypress.$('.segmented button.active:contains("List")').length > 0;
            if (!isActive) {
                cy.contains('.segmented button', 'List').click({ force: true });
                cy.contains('.segmented button.active', 'List', { timeout: 20000 }).should('be.visible');
            }
        }
    });
}

function waitForTxCards(min = 0): void {
    ensureListView();
    cy.get('.tx-list', { timeout: 20000 }).should('exist');
    cy.get('.tx-list .tx-card', { timeout: 20000 }).should('have.length.at.least', min);
}

function openFab(): Cypress.Chainable<void> {
    cy.get('.fab-container', { timeout: 20000 }).should('exist');
    cy.get('.fab-main', { timeout: 20000 }).scrollIntoView().should('be.visible');

    const ensureOpen = (tries = 10): Cypress.Chainable<void> =>
        cy.get('.fab-actions', { timeout: 5000 }).then(($el: JQuery<HTMLElement>) => {
            const cs = getComputedStyle($el[0]);
            const isInteractive = parseFloat(cs.opacity || '0') > 0.01 && cs.pointerEvents !== 'none';
            if (isInteractive) return cy.wrap(undefined);

            if (tries <= 0) throw new Error('FAB actions never became interactive (opacity/pointer-events still blocking).');

            cy.get('.fab-main').click({ force: true });
            return cy.wait(200).then(() => ensureOpen(tries - 1));
        });

    return ensureOpen();
}

function clickFabAction(index: number): Cypress.Chainable<void> {
    return openFab().then(() => {
        cy.get('.fab-actions .fab-action', { timeout: 5000 })
            .eq(index)
            .click({ force: true });
    });
}

function selectFirstCategoryOption(): Cypress.Chainable<void> {
    return cy.get('select[name="categoryId"]', { timeout: 20000 })
        .should('be.visible')
        .then(($sel: JQuery<HTMLElement>) => {
            const $opts = $sel.find('option');
            const txt = ($opts.eq(1).text() || '').trim();
            if (!txt) throw new Error('No categories available. Seed at least one category in the backend.');
            cy.wrap($sel).select(txt);
        });
}

function focusBySearch(note: string): Cypress.Chainable<JQuery<HTMLElement>> {
    return cy.get('.filters .search', { timeout: 10000 })
        .clear()
        .type(note)
        .wait(150);
}

function goToFirstPage(): Cypress.Chainable<void> {
    return cy.get('body').then(($b: JQuery<HTMLElement>) => {
        const $prev = $b.find('nav.pagination button:contains("◀")');
        if ($prev.length && !$prev.prop('disabled')) {
            cy.wrap($prev).click();
            return cy.wait(120).then(goToFirstPage);
        }
        return cy.wrap(undefined);
    });
}

function findCardAcrossPages(note: string, maxPages = 20, timeoutMs = 10000): Cypress.Chainable<JQuery<HTMLElement>> {
    const selector = `.tx-list .tx-card:contains("${note}")`;

    function scanPage(page = 0): Cypress.Chainable<JQuery<HTMLElement> | null> {
        return cy.get('body').then(($b: JQuery<HTMLElement>) => {
            const found = $b.find(selector);
            if (found.length) return cy.contains('.tx-list .tx-card', note) as any;

            const $next = $b.find('nav.pagination button:contains("▶")');
            const canNext = $next.length && !$next.prop('disabled') && page + 1 < maxPages;
            if (!canNext) return null;

            cy.wrap($next)
                .should('exist')
                .should('be.visible')
                .click({ force: true });
            return cy.wait(150).then(() => scanPage(page + 1));
        });
    }

    function scanAllPages(): Cypress.Chainable<JQuery<HTMLElement> | null> {
        return goToFirstPage().then(() => scanPage(0));
    }

    const start = Date.now();
    function retry(): Cypress.Chainable<JQuery<HTMLElement>> {
        return scanAllPages().then((res) => {
            if (res) return res;
            if (Date.now() - start > timeoutMs) throw new Error(`Card with note "${note}" not found within ${timeoutMs}ms`);
            return cy.wait(300).then(retry);
        });
    }

    return retry();
}

function createTransactionUI(args: { amount: number; type: 'Expense' | 'Income'; note: string }): Cypress.Chainable<void> {
    const { amount, type, note } = args;

    return clickFabAction(0).then(() => {
        cy.get('.tx-modal form', { timeout: 10000 }).within(() => {
            cy.get('input[name="amount"]').clear().type(String(amount));
            selectFirstCategoryOption();
            cy.get('select[name="type"]').select(type);
            cy.get('textarea[name="note"]').clear().type(note);
            cy.contains('button', 'Save').click();
        });

        cy.wait('@postTx');
        cy.wait('@getTx');

        cy.get('.tx-modal', { timeout: 10000 }).should('not.exist');
    });
}

function createPlannedUI(args: { amount: number; title: string }): Cypress.Chainable<void> {
    const { amount, title } = args;

    return clickFabAction(1).then(() => {
        cy.get('.tx-modal form', { timeout: 10000 }).within(() => {
            cy.get('input[name="amount"]').clear().type(String(amount));
            selectFirstCategoryOption();
            cy.get('input[name="title"]').clear().type(title);
            const d = new Date();
            d.setDate(d.getDate() + 5);
            cy.get('input[name="date"]').invoke('val', d.toISOString().slice(0, 10)).trigger('input');
            cy.contains('button', 'Save').click();
        });

        cy.get('.tx-modal', { timeout: 10000 }).should('not.exist');
        cy.get('.tx-list', { timeout: 20000 }).should('exist');
    });
}

function seedExactlyN(n: number): Cypress.Chainable<void> {
    const minOnPage = Math.min(n, PAGE_SIZE);

    return cy
        .get('.tx-list', { timeout: 20000 })
        .should('exist')
        .then(($list: JQuery<HTMLElement>) => {
            const have = $list.find('.tx-card').length;
            const need = Math.max(0, n - have);

            if (need > 0) {
                Cypress.log({ name: 'Seed', message: `Creating ${need} transactions` });
                return Cypress._.range(need).reduce((chain) => {
                    return chain
                        .then(() =>
                            createTransactionUI({
                                amount: 10 + Math.floor(Math.random() * 90),
                                type: 'Expense',
                                note: `${NOTE_PREFIX}${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
                            }),
                        )
                        .then(() => cy.wait(80));
                }, cy.wrap(null) as Cypress.Chainable);
            }
            return cy.wrap(undefined);
        })
        .then(() => {
            cy.get('.tx-list .tx-card', { timeout: 30000 }).should('have.length.at.least', minOnPage);
            if (n > PAGE_SIZE) cy.get('nav.pagination', { timeout: 30000 }).should('be.visible');
        });
}

function setRecurring(on: boolean): Cypress.Chainable<void> {
    return cy.get('body').then(($b: JQuery<HTMLElement>) => {
        const $chk = $b.find('.filters input[type="checkbox"]') as JQuery<HTMLInputElement>;
        if (!$chk.length) return;

        const checked = Boolean($chk.prop('checked'));
        if (on !== checked) {
            if (on) cy.wrap($chk).check({ force: true });
            else cy.wrap($chk).uncheck({ force: true });
        }
    }) as any;
}

function loginThroughKeycloakUI(): Cypress.Chainable<void> {
    const baseOrigin = String(Cypress.config('baseUrl')).replace(/\/$/, '');
    const kcOrigin = requireEnv('keycloakUrl');
    const user = requireEnv('keycloakUser');
    const pass = requireEnv('keycloakPass');

    return cy.visit(TX_PATH, { failOnStatusCode: false }).then(() => {
        cy.origin(kcOrigin, { args: { user, pass } }, ({ user, pass }) => {
            cy.get('input#username, input[name="username"]', { timeout: 30000 }).should('be.visible').type(user);
            cy.get('input#password, input[name="password"]').type(pass, { log: false });
            cy.get('input[name="login"], button[type="submit"]').click();
        });

        cy.location('origin', { timeout: 30000 }).should('eq', baseOrigin);
        cy.contains('.tx-header h1', 'Transactions', { timeout: 30000 }).should('be.visible');
    });
}

function visitTransactions(): Cypress.Chainable<void> {
    const baseOrigin = String(Cypress.config('baseUrl')).replace(/\/$/, '');
    const kcOrigin = requireEnv('keycloakUrl');

    return cy.visit(TX_PATH, { failOnStatusCode: false }).then(() => {
        cy.location('origin', { timeout: 30000 }).then((origin) => {
            if (origin !== baseOrigin) {
                cy.origin(kcOrigin, () => {
                    cy.get('input#username, input[name="username"]', { timeout: 30000 })
                        .should('be.visible')
                        .type(Cypress.env('keycloakUser') as string);
                    cy.get('input#password, input[name="password"]').type(Cypress.env('keycloakPass') as string, { log: false });
                    cy.get('input[name="login"], button[type="submit"]').click();
                });
                cy.location('origin', { timeout: 30000 }).should('eq', baseOrigin);
            }
        });

        cy.contains('.tx-header h1', 'Transactions', { timeout: 30000 }).should('be.visible');
        cy.get('.fab-main', { timeout: 30000 }).should('be.visible');
    });
}

function deleteNextCard(): Cypress.Chainable<void> {
    return cy.get('body').then(($body: JQuery<HTMLElement>) => {
        const $cards = $body.find('.tx-list .tx-card');

        if (!$cards.length) {
            const $next = $body.find('nav.pagination button:contains("▶")');
            if ($next.length && !$next.prop('disabled')) {
                cy.wrap($next).click();
                return cy.wait(120).then(() => deleteNextCard());
            }
            const $prev = $body.find('nav.pagination button:contains("◀")');
            if ($prev.length && !$prev.prop('disabled')) {
                cy.wrap($prev).click();
                return cy.wait(120).then(() => deleteNextCard());
            }
            return cy.wrap(undefined);
        }

        cy.wrap($cards.first()).click();
        cy.get('.tx-modal').within(() => {
            cy.contains('button', 'Delete').click();
            cy.contains('button', 'Delete').click();
        });
        cy.get('.tx-modal').should('not.exist');

        return cy.wait(120).then(() => deleteNextCard());
    });
}

function deleteAllTransactions(): Cypress.Chainable<void> {
    ensureListView();
    return goToFirstPage().then(() =>
        cy.get('body').then(($body: JQuery<HTMLElement>) => {
            if (!$body.find('.tx-list .tx-card').length) {
                cy.log('No transactions to delete.');
                return cy.wrap(undefined);
            }
            cy.log('Deleting all transactions...');
            return deleteNextCard();
        }),
    );
}

const validateSession = () => {
    visitTransactions();
    cy.get('.tx-list', { timeout: 20000 }).should('exist');
};
const sessionOpts: Cypress.SessionOptions = {
    validate: validateSession,
    cacheAcrossSpecs: true,
};

describe('Transactions – e2e (manual seed then verify)', () => {
    before(() => {
        Cypress.session.clearAllSavedSessions();
        aliasApi();
        cy.session('keycloak-session', loginThroughKeycloakUI, sessionOpts);

        visitTransactions();
        ensureListView();
        deleteAllTransactions()
            .then(() => seedExactlyN(12))
            .then(() => {
                cy.get('.tx-list .tx-card', { timeout: 30000 })
                    .should('have.length.at.least', Math.min(12, PAGE_SIZE));
            });
    });

    beforeEach(() => {
        aliasApi();
        cy.session('keycloak-session', loginThroughKeycloakUI, sessionOpts);
        cy.visit('/');
        visitTransactions();
        ensureListView();
    });

    after(() => {
        visitTransactions();
        ensureListView();
        deleteAllTransactions();
    });

    it('paginates next/prev', function () {
        cy.get('nav.pagination', { timeout: 20000 }).should('be.visible');

        cy.contains('nav.pagination button', '▶').then(($next: JQuery<HTMLElement>) => {
            if ($next.is(':disabled')) this.skip();
            cy.wrap($next).click();
        });

        cy.contains('nav.pagination span', /Page\s+2\s*\/\s*\d+/, { timeout: 10000 }).should('be.visible');

        cy.contains('nav.pagination button', '◀').should('not.be.disabled').click();
        cy.contains('nav.pagination span', /Page\s+1\s*\/\s*\d+/).should('be.visible');
    });

    it('filters by Type, Search, Min/Max and resets', () => {
        const uniqueNote = nextNote();
        createTransactionUI({ amount: 12.34, type: 'Expense', note: uniqueNote });

        cy.get('.filters select').first().select('Expense');
        cy.get('.tx-list .tx-card.income').should('have.length', 0);

        focusBySearch(uniqueNote);
        findCardAcrossPages(uniqueNote).should('exist');

        cy.get('.filters input[type="number"]').eq(0).clear().type('1000');
        cy.get('.tx-list .tx-card').should('have.length', 0);

        cy.get('.filters input[type="number"]').eq(0).clear();
        cy.get('.filters input[type="number"]').eq(1).clear().type('12.34');
        findCardAcrossPages(uniqueNote).should('exist');

        cy.get('.filters .btn-reset').click();
        waitForTxCards(1);
    });

    it('views, edits, and saves a transaction', () => {
        const note = nextNote();
        createTransactionUI({ amount: 9.99, type: 'Expense', note });

        findCardAcrossPages(note)
            .should('exist')
            .should('be.visible')
            .click({ force: true });
        cy.get('.tx-modal').within(() => {
            cy.contains('h2', 'Transaction').should('be.visible');
            cy.contains('button', 'Edit').click();
        });

        cy.get('.tx-modal form').within(() => {
            cy.get('input[name="amount"]').clear().type('15.55');
            cy.get('select[name="type"]').select('Expense');
            cy.contains('button', 'Save').click();
        });

        cy.wait('@patchTx');
        cy.wait('@getTx');
        cy.get('.tx-modal').should('not.exist');

        focusBySearch(note);
        cy.contains('.tx-card', note, { timeout: 10000 })
            .should(($card) => {
                const txt = $card.find('.amount').text().replace(/\s/g, '');
                expect(txt).to.match(/^€?15[.,]55$/);
            });

        cy.get('.filters .btn-reset').click();
    });

    it('deletes a transaction', () => {
        const note = nextNote();
        createTransactionUI({ amount: 7.77, type: 'Expense', note });

        focusBySearch(note);
        cy.contains('.tx-card', note, { timeout: 10000 }).click();

        cy.get('.tx-modal').within(() => cy.contains('button', 'Delete').click());
        cy.get('.tx-modal').within(() => cy.contains('button', 'Delete').click());

        cy.wait('@delTx');
        cy.wait('@getTx');
        cy.get('.tx-modal').should('not.exist');

        focusBySearch(note);
        cy.contains('.tx-card', note).should('not.exist');

        cy.get('.filters .btn-reset').click();
    });

    it('shows planned item in List (Recurring) and Calendar', () => {
        const title = nextPlanTitle();
        createPlannedUI({ amount: 300, title });

        cy.get('.filters input[type="checkbox"]').check({ force: true });
        cy.contains('.tx-list .tx-card', title, { timeout: 10000 }).should('exist');

        cy.contains('.segmented button', 'Calendar').click().should('have.class', 'active');
        cy.get('app-calendar-view .mini', { timeout: 10000 }).should('have.length.at.least', 1).first().click();
        cy.get('.tx-modal').should('be.visible').and('contain.text', 'Transaction');
        cy.get('.tx-modal .btn-x').click();
    });

    after(() => {
        aliasApi();
        cy.session('keycloak-session', loginThroughKeycloakUI, sessionOpts);

        visitTransactions()
            .then(() => ensureListView())
            .then(() =>
                cy.get('body').then(($b) => {
                    const $body = $b as JQuery<HTMLElement>;
                    const $btn = $body.find('.filters .btn-reset');
                    if ($btn.length) cy.wrap($btn).click({ force: true });
                }),
            )
            .then(() => deleteAllTransactions())
            .then(() => setRecurring(true))
            .then(() => deleteAllTransactions())
            .then(() => {
                cy.get('body').then(($b) => {
                    const $body = $b as JQuery<HTMLElement>;
                    const $prev = $body.find('nav.pagination button:contains("◀")');
                    if ($prev.length && !$prev.prop('disabled')) cy.wrap($prev).click();
                });
                cy.get('.tx-list .tx-card', { timeout: 10000 }).should('have.length', 0);
            });
    });
});
