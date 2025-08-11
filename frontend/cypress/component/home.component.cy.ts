/// <reference types="cypress" />

import { of, BehaviorSubject } from 'rxjs';
import { HomeComponent } from '../../src/app/home/home.component';
import { HomeService } from '../../src/app/home/home.service';
import { CategoryService } from '../../src/app/settings/category.service';
import { NotificationService } from '../../src/app/services/notification.service';
import { ThemeService } from '../../src/app/theme.service';

import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

const mockHomeApi = {
    getBalance: cy.stub().returns(of({ success: true, data: 123.45 })),
    getRecent: cy.stub().returns(of({
        success: true,
        data: [
            { id: 1, type: 'E', amount: -10, categoryId: 2, note: 'Coffee', txTime: new Date().toISOString() },
            { id: 2, type: 'I', amount: 100, categoryId: 1, note: 'Salary', txTime: new Date().toISOString() },
        ]
    })),
    getAllBudgets: cy.stub().returns(of({
        success: true,
        data: { items: [{ categoryId: 2, amount: 200 }, { categoryId: 3, amount: 0 }] }
    })),
    getByDateRange: cy.stub().returns(of({
        success: true,
        data: [
            { txTime: new Date().toISOString(), type: 'E', amount: 20, categoryId: 2 },
            { txTime: new Date().toISOString(), type: 'I', amount: 45, categoryId: 1 },
        ]
    })),
    getDailyGraph: cy.stub().returns(of({ success: true, data: { '2025-08-01': -10, '2025-08-02': 20 } })),
    getMonthlyGraph: cy.stub().returns(of({ success: true, data: { Jan: 100, Feb: -50 } })),
    getUpcomingBills: cy.stub().returns(of({
        success: true,
        data: [{ dueDate: new Date().toISOString() }, { dueDate: new Date().toISOString() }]
    })),
    quickAdd: cy.stub().returns(of({ success: true, message: 'Saved' })),
};

const mockCatApi = {
    listAll: cy.stub().returns(of({
        success: true, data: [
            { id: 1, name: 'Salary' }, { id: 2, name: 'Groceries' }
        ]
    }))
};

const mockNotify = { notify: cy.stub() };
const mockTheme = { initTheme: cy.stub().returns('light'), setTheme: cy.stub() };

// ✅ BehaviorSubject so we can read current value safely
const keycloakEvent$ = new BehaviorSubject<any>({ type: KeycloakEventType.Ready, args: [{}] });
const mockKeycloakSignal = () => keycloakEvent$.value;

const mockKeycloak = {
    loadUserProfile: cy.stub().resolves({ username: 'daniel' })
} as unknown as Keycloak;

describe('HomeComponent', () => {
    beforeEach(() => {
        keycloakEvent$.next({ type: KeycloakEventType.Ready, args: [{}] });

        // ✅ Stub camera APIs in the AUT window (typed as any to dodge TS confusion)
        cy.window().then((win) => {
            const nav = (win as any).navigator as any;
            nav.mediaDevices = nav.mediaDevices || {};
            nav.mediaDevices.getUserMedia = cy.stub().rejects(new Error('no camera in CT'));
            nav.mediaDevices.enumerateDevices = cy.stub().resolves([]);
        });
    });

    it('renders header, balance, recent activity & can open Quick Add modal', () => {
        cy.mount(HomeComponent, {
            providers: [
                { provide: HomeService, useValue: mockHomeApi },
                { provide: CategoryService, useValue: mockCatApi },
                { provide: NotificationService, useValue: mockNotify },
                { provide: ThemeService, useValue: mockTheme },
                { provide: KEYCLOAK_EVENT_SIGNAL, useValue: mockKeycloakSignal },
                { provide: Keycloak, useValue: mockKeycloak },
            ]
        });

        cy.contains('.home__greeting', 'Hey, daniel!').should('exist');

        // Don’t assume a specific currency symbol; just assert a number is shown
        cy.contains('.card__label', 'Balance')
            .parent()
            .invoke('text')
            .should((t) => expect(t).to.match(/\d/));

        cy.get('.home__history li').should('have.length.at.least', 1);

        // Chart controls
        cy.contains('button', 'Daily').click().should('have.class', 'active');
        cy.contains('button', 'Monthly').click().should('have.class', 'active');
        cy.get('.chart-card canvas').should('exist');

        // Open Quick Add and save
        cy.contains('.home__actions .action-btn', 'Add').click();
        cy.contains('.modal h2', 'Quick Add').should('be.visible');
        cy.get('#quick-amount').clear().type('12.34');
        cy.get('#quick-type').select('Expense');
        cy.contains('.modal-actions button', 'Save').click();
        cy.wrap(mockHomeApi.quickAdd).should('have.been.called');
        cy.contains('.modal h2', 'Quick Add').should('not.exist');
    });

    it('opens QR modal and shows waiting text', () => {
        cy.mount(HomeComponent, {
            providers: [
                { provide: HomeService, useValue: mockHomeApi },
                { provide: CategoryService, useValue: mockCatApi },
                { provide: NotificationService, useValue: mockNotify },
                { provide: ThemeService, useValue: mockTheme },
                { provide: KEYCLOAK_EVENT_SIGNAL, useValue: mockKeycloakSignal },
                { provide: Keycloak, useValue: mockKeycloak },
            ]
        });

        cy.contains('.home__actions .action-btn', 'Scan QR').click();
        cy.contains('.modal h2', 'Scan QR').should('be.visible');
        cy.contains('.scan-info p', 'Waiting for QR…').should('exist');
    });
});
