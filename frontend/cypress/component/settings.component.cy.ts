/// <reference types="cypress" />

import { of } from 'rxjs';
import { Component } from '@angular/core';
import { provideRouter } from '@angular/router';

import { SettingsComponent } from '../../src/app/settings/settings.component';
import { SettingsService } from '../../src/app/settings/settings.service';
import { CategoryService } from '../../src/app/settings/category.service';
import { ThemeService } from '../../src/app/theme.service';
import { NotificationService } from '../../src/app/services/notification.service';

import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

@Component({ standalone: true, template: '<p>Settings route stub</p>' })
class SettingsStub { }

let settingsApi: any;
let categoryApi: any;
let themeSvc: any;
let notifySvc: any;
let keycloakSignal: any;
let keycloak: Keycloak;

const categories = [
    { id: 1, name: 'Groceries' },
    { id: 2, name: 'Salary' },
    { id: 3, name: 'Transport' },
];

const budgetsPage = {
    total: 2,
    items: [
        { categoryId: 1, amount: 50 },
        { categoryId: 2, amount: 80 },
    ],
};

const goalResp = { success: true, data: { targetAmt: 1000, targetDate: new Date('2025-12-01').toISOString() } };
const credsResp = { success: true, data: { apiKey: 'a1', secretKey: 'b2' } };

describe('SettingsComponent', () => {
    beforeEach(() => {
        settingsApi = {
            getBudgets: cy.stub().callsFake((page: number, size: number) => of({ success: true, data: budgetsPage })),
            deleteBudgets: cy.stub().returns(of({ success: true, message: 'Deleted' })),
            createBudgets: cy.stub().returns(of({ success: true, message: 'Created' })),
            updateBudgets: cy.stub().returns(of({ success: true, message: 'Updated' })),
            getSavingsGoal: cy.stub().returns(of(goalResp)),
            upsertSavingsGoal: cy.stub().returns(of({ success: true, data: goalResp.data, message: 'Saved' })),
            deleteSavingsGoal: cy.stub().returns(of({ success: true, message: 'Deleted' })),
            getBinanceCredentials: cy.stub().returns(of(credsResp)),
            upsertBinanceCredentials: cy.stub().returns(of({ success: true, message: 'Saved' })),
            deleteBinanceCredentials: cy.stub().returns(of({ success: true, message: 'Deleted' })),
        };

        categoryApi = {
            listAll: cy.stub().returns(of({ success: true, data: categories })),
        };

        themeSvc = { initTheme: cy.stub().returns('light'), setTheme: cy.stub() };
        notifySvc = { notify: cy.stub() };

        keycloakSignal = () => ({ type: KeycloakEventType.Ready, args: [{}] });
        keycloak = {
            loadUserProfile: cy.stub().resolves({ username: 'daniel', email: 'd@x' }),
            logout: cy.stub().resolves(),
        } as unknown as Keycloak;

        cy.window().then((w) => {
            (w as any).OneSignalDeferred = [];
        });
    });

    const mountSettings = () =>
        cy.mount(SettingsComponent, {
            providers: [
                provideRouter([{ path: 'settings', component: SettingsStub }]),
                { provide: SettingsService, useValue: settingsApi },
                { provide: CategoryService, useValue: categoryApi },
                { provide: ThemeService, useValue: themeSvc },
                { provide: NotificationService, useValue: notifySvc },
                { provide: KEYCLOAK_EVENT_SIGNAL, useValue: keycloakSignal },
                { provide: Keycloak, useValue: keycloak },
            ],
        });

    it('loads budgets and supports bulk delete', () => {
        mountSettings();

        cy.contains('h2', 'Existing Budgets')
            .parent('section')
            .within(() => {
                cy.get('tbody tr').should('have.length', 2);

                cy.get('.budget-toolbar .btn-delete').as('deleteBtn');
                cy.get('@deleteBtn').should('be.disabled');

                cy.get('tbody tr').first().find('input[type="checkbox"]').click({ force: true });

                cy.get('@deleteBtn').should('not.be.disabled').click();
            });

        cy.wrap(settingsApi.deleteBudgets).should('have.been.calledWith', [1]);
    });

    it('filters budgets by search term', () => {
        mountSettings();

        cy.contains('h2', 'Existing Budgets')
            .parent('section')
            .within(() => {
                cy.get('tbody tr').should('have.length', 2);
                cy.get('input[placeholder="Category…"]').type('gro');
                cy.get('tbody tr').should('have.length', 1).first().should('contain.text', 'Groceries');
            });
    });

    it('adds a new budget (createBudgets) and resets rows after save', () => {
        mountSettings();

        cy.contains('h2', 'Add New Budgets')
            .parent('section')
            .within(() => {
                cy.contains('button', '+ Add another').click();
                cy.get('.budget-row').should('have.length', 2);

                cy.get('.budget-row').eq(0).within(() => {
                    cy.get('select').select('Transport');
                    cy.get('input[type="number"]').clear().type('123');
                });

                cy.contains('button', 'Save New Budgets').should('not.be.disabled').click();
                cy.get('.budget-row').should('have.length', 1);
            });

        cy.wrap(settingsApi.createBudgets).should('have.been.calledWith', [{ categoryId: 3, amount: 123 }]);
    });

    it('updates an existing budget (updateBudgets) when choosing an existing category', () => {
        mountSettings();

        cy.contains('h2', 'Add New Budgets')
            .parent('section')
            .within(() => {
                cy.get('.budget-row').should('have.length', 1);
                cy.get('.budget-row').eq(0).within(() => {
                    cy.get('select').select('Groceries'); // existing
                    cy.get('input[type="number"]').clear().type('75');
                });

                cy.contains('button', 'Save New Budgets').click();
            });

        cy.wrap(settingsApi.updateBudgets).should('have.been.calledWith', [{ categoryId: 1, amount: 75 }]);
    });

    it('saves theme preference and notifies', () => {
        mountSettings();

        cy.contains('h2', 'Preferences')
            .parent('section')
            .within(() => {
                cy.contains('label', 'Theme').parent().find('select').select('dark');
                cy.contains('button', 'Save Preferences').should('not.be.disabled').click();
            });

        cy.wrap(themeSvc.setTheme).should('have.been.calledWith', 'dark');
        cy.wrap(notifySvc.notify).should('have.been.calledWith', 'Preferences saved', 'success');
    });

    it('saves and deletes Savings Goal', () => {
        mountSettings();

        cy.contains('h2', 'Savings Goal')
            .parent('section')
            .within(() => {
                cy.contains('label', 'Target Amount').parent().find('input[type="number"]').clear().type('1500');
                cy.contains('label', 'By').parent().find('input[type="date"]').clear().type('2025-12-31');

                cy.contains('button', 'Save Goal').should('not.be.disabled').click();
                cy.contains('button', 'Delete Goal').click();
            });

        cy.wrap(settingsApi.upsertSavingsGoal).should('have.been.called');
        cy.wrap(settingsApi.deleteSavingsGoal).should('have.been.called');
    });

    it('handles Binance credentials save & delete', () => {
        mountSettings();

        cy.contains('h2', 'Binance API Credentials')
            .parent('section')
            .within(() => {
                cy.get('input[type="text"]').clear().type('newKey');
                cy.get('input[type="password"]').clear().type('newSecret');
                cy.contains('button', 'Save Credentials').should('not.be.disabled').click();
                cy.contains('button', 'Delete Credentials').should('not.be.disabled').click();
            });

        cy.wrap(settingsApi.upsertBinanceCredentials)
            .should('have.been.calledWith', { apiKey: 'newKey', secretKey: 'newSecret' });
        cy.wrap(settingsApi.deleteBinanceCredentials).should('have.been.called');
    });
});
