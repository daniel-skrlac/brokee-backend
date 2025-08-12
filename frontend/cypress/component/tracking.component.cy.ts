/// <reference types="cypress" />

import { of } from 'rxjs';
import { TrackingComponent } from '../../src/app/tracking/tracking.component';
import { TrackingService } from '../../src/app/tracking/tracking.service';
import { BinanceService } from '../../src/app/tracking/binance.service';
import { ThemeService } from '../../src/app/theme.service';

import * as L from 'leaflet';

let svc: any;
let binance: any;
let theme: any;

const spendingVsIncome = [
    { month: 'Jan', expenses: 100, income: 200 },
    { month: 'Feb', expenses: 150, income: 300 },
];

const topLocations = [
    { latitude: 45.81, longitude: 15.98, label: 'Zagreb Center' },
    { latitude: 45.80, longitude: 15.95, label: 'Jarun' },
    { latitude: 45.84, longitude: 16.00, label: 'Maksimir' },
];

const dailyExpenses = {
    '2025-07-01': 12,
    '2025-07-02': 30,
    '2025-07-03': 18,
};

const categoryBreakdown = [
    { category: 'Groceries', amount: 120 },
    { category: 'Transport', amount: 60 },
];

const portfolio = {
    totalEurValue: 1234.56,
    topMarketCoins: [{ symbol: 'BTCUSDT', lastPrice: 62000, priceChangePercent: 2.5 }],
    myCoins: [
        {
            symbol: 'ETH',
            free: '0.50',
            locked: '0.00',
            eurValue: 900,
            trades: [
                { id: 1, isBuyer: true, price: '1800', qty: '0.1', time: new Date().toISOString() },
            ],
        },
    ],
};

describe('TrackingComponent', () => {
    beforeEach(() => {
        svc = {
            getSpendingVsIncome: cy.stub().returns(of({ success: true, data: spendingVsIncome })),
            getTopLocations: cy.stub().returns(of({ success: true, data: topLocations })),
            getDailyExpenses: cy.stub().returns(of({ success: true, data: dailyExpenses })),
            getCategoryBreakdown: cy.stub().returns(of({ success: true, data: categoryBreakdown })),
        };

        binance = {
            getCredentials: cy.stub().returns(of({ success: true, data: { ok: true } })),
            getPortfolio: cy.stub().returns(of({ success: true, data: portfolio })),
        };

        theme = { initTheme: cy.stub().returns('light') };

        const fakeMap = {
            setView: cy.stub().as('setView').returnsThis(),
            getZoom: cy.stub().returns(12),
        };
        const fakeTileLayer = { addTo: cy.stub().returns(fakeMap) };
        const fakeMarker = {
            addTo: cy.stub().returnsThis(),
            setLatLng: cy.stub().as('setLatLng').returnsThis(),
        };

        cy.stub(L, 'map').callsFake(() => fakeMap);
        cy.stub(L, 'tileLayer').returns(fakeTileLayer as any);
        cy.stub(L, 'marker').returns(fakeMarker as any);
    });

    const mountTracking = () =>
        cy.mount(TrackingComponent, {
            providers: [
                { provide: TrackingService, useValue: svc },
                { provide: BinanceService, useValue: binance },
                { provide: ThemeService, useValue: theme },
            ],
        });

    it('renders charts and computes savings rate (gauge center shows %)', () => {
        mountTracking();

        cy.contains('h1', 'Statistics').should('exist');
        cy.contains('h2', 'Cash-Flow Funnel In Current Year').should('exist');
        cy.contains('h2', 'Savings Rate').should('exist');
        cy.contains('h2', '30-Day Avg Spend').should('exist');
        cy.contains('h2', 'Spending vs Income').should('exist');
        cy.contains('h2', 'Category Breakdown').should('exist');

        cy.get('.funnel-section canvas').should('exist');
        cy.get('.gauge-section canvas').should('exist');
        cy.get('.rolling-avg canvas').should('exist');

        cy.get('.gauge-center .gauge-value').should('contain.text', '50%');
    });

    it('shows "no locations" fallback when no top locations', () => {
        svc.getTopLocations.returns(of({ success: true, data: [] }));
        mountTracking();

        cy.contains('h2', 'Top 3 Spending Locations').should('exist');
        cy.get('.map-section .no-data').should('contain.text', 'There are no locations to be shown.');
    });

    it('cycles location label with map controls (◀ ▶)', () => {
        mountTracking();

        cy.get('.map-section .map-controls span')
            .should('contain.text', 'Zagreb Center');

        cy.get('.map-section .map-controls button').contains('▶').click();
        cy.get('.map-section .map-controls span')
            .should('contain.text', 'Jarun');

        cy.get('.map-section .map-controls button').contains('▶').click();
        cy.get('.map-section .map-controls span')
            .should('contain.text', 'Maksimir');

        cy.get('.map-section .map-controls button').contains('◀').click();
        cy.get('.map-section .map-controls span')
            .should('contain.text', 'Jarun');

        cy.wrap(L.map as any).should('have.been.called');
    });

    it('enables Binance button when credentials exist and opens modal with portfolio', () => {
        mountTracking();

        cy.get('.binance-btn').should('not.be.disabled').click();

        cy.contains('.modal h2', 'Your Binance Portfolio').should('be.visible');

        cy.contains('.modal__subtitle', 'Total Value').should('contain.text', '€');
        cy.contains('h3', 'Top Market Movers').should('exist');
        cy.get('.binance-table tbody tr').should('have.length.at.least', 1);

        cy.wrap(binance.getPortfolio).should('have.been.calledWith', 'EUR');
    });

    it('disables Binance button when no credentials', () => {
        binance.getCredentials.returns(of({ success: true, data: null }));
        mountTracking();

        cy.get('.binance-btn').should('be.disabled');
    });
});
