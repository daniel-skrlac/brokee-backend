import {
  Component, OnInit, ViewChild, ElementRef
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { combineLatest, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  startOfDay, startOfMonth, endOfMonth, isSameMonth,
  format,
  addDays
} from 'date-fns';

import { CalendarViewComponent } from './calendar-view.component';
import {
  TxResponseDTO, PlannedTxResponseDTO,
  FullTxRequestDTO, PlannedTxRequestDTO,
  CategoryResponseDTO, ServiceResponseDTO
} from '../api/dtos';
import { TransactionService } from './transaction.service';
import { PlannedTxService } from './planned-tx.service';
import { CategoryService } from '../settings/category.service';
import { RevolutService } from './revolut.service';
import { safeDate } from '../utils/date-safe';
import { ThemeService } from '../theme.service';
import { NotificationService } from '../services/notification.service';

const ICONS: Record<string, string> = {
  'ATM Fee': '🏧', 'Business': '💼', 'Cash Withdrawal': '💳', 'Clothing': '👚',
  'Dining Out': '🍽️', 'Education': '🎓', 'Entertainment': '🎭', 'Fuel': '⛽',
  'General': '📦', 'Gifts & Donations': '🎁', 'Groceries': '🛒', 'Health & Fitness': '💪',
  'Home Maintenance': '🛠️', 'Insurance': '🛡️', 'Internet': '🌐', 'Investments': '💹',
  'Kids': '🧸', 'Miscellaneous': '✨', 'Mobile Phone': '📱', 'Personal Care': '🧴',
  'Pets': '🐾', 'Rent': '🏠', 'Revolut': '💳', 'Savings': '💰', 'Shopping': '🛍️',
  'Streaming Services': '📺', 'Subscriptions': '🔄', 'Taxes': '🧾',
  'Transportation': '🚗', 'Travel': '✈️', 'Utilities': '💡'
};
const DEFAULT_ICON = '💳';
const iconFor = (n?: string) => ICONS[n ?? ''] ?? DEFAULT_ICON;

export interface UITransaction {
  id: string; type: 'expense' | 'income'; amount: number;
  categoryId?: number | null; category: string; date: Date; note?: string;
  planned: boolean; categoryIcon: string; hasFull: boolean;
}
type ModalMode =
  | 'view' | 'delete'
  | 'quickEdit' | 'fullEdit'
  | 'planEdit' | 'newTx' | 'newPlan';

@Component({
  selector: 'app-transactions',
  standalone: true,
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.scss'],
  imports: [CommonModule, FormsModule, DecimalPipe, CalendarViewComponent]
})
export class TransactionComponent implements OnInit {

  themes = ['light', 'dark'] as const;
  theme: 'light' | 'dark' = "light";

  private _mode: 'list' | 'calendar' = 'list';
  get mode() { return this._mode; }
  setMode(m: 'list' | 'calendar') { this.mode = m; }


  calendarItems: UITransaction[] = [];
  private lastCalendarKey = '';

  private items: UITransaction[] = [];
  viewTx: UITransaction[] = [];
  groups: Record<string, UITransaction[]> = {};

  readonly size = 10;
  currentPage = 0;
  private lastFilterKey = '';
  private filteredPages = 1;
  private totalPages = 1;

  fabOpen = false;
  searchTxt = '';
  f = {
    type: '', from: '', to: '',
    min: null as number | null, max: null as number | null,
    recurring: false
  };

  modalMode: ModalMode = 'view';
  selected: UITransaction | null = null;
  editForm: any = {}; loadingEdit = false;

  categories: CategoryResponseDTO[] = [];

  private lastQueryKey = '';

  @ViewChild('pdfInput') pdfInput!: ElementRef<HTMLInputElement>;

  constructor(
    private txApi: TransactionService,
    private planApi: PlannedTxService,
    private catApi: CategoryService,
    private revApi: RevolutService,
    private themeService: ThemeService,
    private notifications: NotificationService
  ) { }

  ngOnInit(): void {
    this.theme = this.themeService.initTheme();

    this.catApi.listAll()
      .pipe(
        catchError(() =>
          of({ data: [] } as unknown as ServiceResponseDTO<CategoryResponseDTO[]>)
        )
      )
      .subscribe(r => {
        this.categories = r.data ?? [];
        this.recalcView();
      });

    this.lastFilterKey = this.computeFilterKey();
    this.fetchPage(0);
    this.loadCalendarMonth();
  }

  set mode(m: 'list' | 'calendar') {
    this._mode = m;
    this.currentPage = 0;
    if (m === 'calendar') {
      this.loadCalendarMonth();
    } else {
      this.recalcView();
    }
  }

  private loadCalendarMonth(forDate: Date = new Date()): void {
    const start = new Date(forDate.getFullYear(), forDate.getMonth(), 1);
    const end = new Date(forDate.getFullYear(), forDate.getMonth() + 1, 0);
    const from = format(start, 'yyyy-MM-dd');
    const to = format(end, 'yyyy-MM-dd');

    const key = `${from}:${to}`;
    this.lastCalendarKey = key;

    this.planApi.list(undefined, from, to)
      .pipe(catchError(() => of({ data: [] } as any)))
      .subscribe(resp => {
        if (this.lastCalendarKey !== key) return;
        this.calendarItems = (resp.data ?? []).map(this.mapPlanned).filter(Boolean);
        if (this.mode === 'calendar') this.recalcView();
      });
  }

  private handleSave(
    obs$: Observable<{ success?: boolean; message?: string }>,
    successCb: () => void,
    okMsg = 'Operation completed'
  ): void {
    obs$.subscribe({
      next: r => {
        const msg = r?.message ?? okMsg;
        const isOk = r?.success ?? true;
        this.notifications.notify(msg, isOk ? 'success' : 'error');
        if (isOk) { successCb(); }
      },
      error: () => this.notifications.notify('Request failed', 'error'),
    });
  }

  private computeFilterKey(): string {
    return JSON.stringify({
      q: (this.searchTxt || '').trim().toLowerCase(),
      f: this.f,
      recurring: this.f.recurring
    });
  }

  onFiltersChanged(): void {
    const next = this.computeFilterKey();
    if (next === this.lastFilterKey) return;
    this.lastFilterKey = next;
    this.currentPage = 0;
    this.fetchFiltered(0);
  }

  private catName(id?: number | null) {
    return this.categories.find(c => c.id === id)?.name ?? 'General';
  }

  fetchPage(page: number) {
    this.fetchFiltered(page);
  }

  private fetchFiltered(page: number) {
    this.currentPage = page;

    const qKey = JSON.stringify({
      page,
      q: (this.searchTxt || '').trim(),
      f: this.f,
      recurring: this.f.recurring
    });
    this.lastQueryKey = qKey;

    if (this.f.recurring) {
      const args = this.buildPlannedQueryArgs();
      this.planApi.page(page, this.size, ...args)
        .pipe(catchError(() => of({ data: { items: [], total: 0 } } as any)))
        .subscribe(resp => {
          if (qKey !== this.lastQueryKey) return;
          const list = (resp.data?.items ?? []).map(this.mapPlanned).filter(Boolean);
          this.items = list;
          this.totalPages = Math.max(1, Math.ceil((resp.data?.total ?? 0) / this.size));
          this.recalcView();
        });
    } else {
      const opts = this.buildTxQueryOpts();
      this.txApi.page(page, this.size, opts)
        .pipe(catchError(() => of({ data: { items: [], total: 0 } } as any)))
        .subscribe(resp => {
          if (qKey !== this.lastQueryKey) return;
          const list = (resp.data?.items ?? []).map(this.mapRegular).filter(Boolean);
          this.items = list;
          this.totalPages = Math.max(1, Math.ceil((resp.data?.total ?? 0) / this.size));
          this.recalcView();
        });
    }
  }

  private mapRegular = (t: TxResponseDTO): UITransaction => {
    const txTime = safeDate(t.txTime);
    if (!txTime) return null as any;
    return {
      id: String(t.id),
      type: t.type === 'E' ? 'expense' : 'income',
      amount: +t.amount,
      categoryId: t.categoryId,
      category: this.catName(t.categoryId),
      date: txTime,
      note: t.note,
      planned: false,
      categoryIcon: iconFor(this.catName(t.categoryId)),
      hasFull: true,
    };
  };
  private mapPlanned = (p: PlannedTxResponseDTO): UITransaction => {
    const due = safeDate(p.dueDate);
    if (!due) return null as any;
    return {
      id: `p-${p.id}`,
      type: p.type === 'E' ? 'expense' : 'income',
      amount: +p.amount,
      categoryId: p.categoryId,
      category: p.title,
      date: due,
      note: '',
      planned: true,
      categoryIcon: iconFor(p.title),
      hasFull: true,
    };
  };

  private get filtersActive(): boolean {
    const { type, from, to, min, max, recurring } = this.f;
    return !!(
      this.searchTxt || type || from || to ||
      min != null || max != null || recurring
    );
  }

  recalcView(): void {
    if (this.mode === 'calendar') {
      this.viewTx = this.calendarItems;
    } else {
      const pageItems = this.f.recurring ? this.items.filter(t => t.planned)
        : this.items.filter(t => !t.planned);
      this.viewTx = pageItems;
      this.groups = pageItems.reduce((m, t) => {
        const k = startOfDay(t.date).toDateString();
        (m[k] ??= []).push(t);
        return m;
      }, {} as Record<string, UITransaction[]>);
    }
  }

  private buildTxQueryOpts() {
    const { type, from, to, min, max } = this.f;
    const qRaw = (this.searchTxt || '').trim();

    const opts: {
      type?: 'expense' | 'income' | 'E' | 'I';
      min?: number; max?: number;
      from?: string; to?: string;
      note?: string; category?: string;
    } = {};

    const rawType = type?.toLowerCase();
    if (rawType === 'expense') opts.type = 'expense';
    else if (rawType === 'income') opts.type = 'income';

    if (min != null) opts.min = min;
    if (max != null) opts.max = max;
    if (from) opts.from = new Date(from + 'T00:00:00Z').toISOString();
    if (to) opts.to = new Date(to + 'T23:59:59.999Z').toISOString();

    if (qRaw) {
      const catHit = this.categories.find(c => c.name.toLowerCase().includes(qRaw.toLowerCase()));
      if (catHit) opts.category = catHit.name;
      else opts.note = qRaw;
    }
    return opts;
  }

  private buildPlannedQueryArgs(): Parameters<PlannedTxService['list']> {
    const { type, from, to, min, max } = this.f;
    const qRaw = (this.searchTxt || '').trim();

    let typeParam: string | undefined;
    const rawType = type?.toLowerCase();
    if (rawType === 'expense') typeParam = 'E';
    else if (rawType === 'income') typeParam = 'I';

    let title: string | undefined;
    let cat: string | undefined;
    if (qRaw) {
      const catHit = this.categories.find(c => c.name.toLowerCase().includes(qRaw.toLowerCase()));
      if (catHit) cat = catHit.name; else title = qRaw;
    }

    return [
      title,
      from || undefined,
      to || undefined,
      typeParam,
      min ?? undefined,
      max ?? undefined,
      cat
    ];
  }

  get displayPages() { return this.totalPages; }

  nextPage() {
    if (this.currentPage + 1 >= this.totalPages) return;
    this.fetchFiltered(this.currentPage + 1);
  }
  prevPage() {
    if (this.currentPage === 0) return;
    this.fetchFiltered(this.currentPage - 1);
  }


  get groupKeys() {
    return Object.keys(this.groups)
      .sort((a, b) => +new Date(b) - +new Date(a));
  }

  clearFilters() {
    this.searchTxt = '';
    this.f = { type: '', from: '', to: '', min: null, max: null, recurring: false };
    this.onFiltersChanged();
  }

  handleDatePick(d: Date) {
    const from = startOfDay(d);
    this.f.from = format(from, 'yyyy-MM-dd');
    this.f.to = format(addDays(from, 1), 'yyyy-MM-dd');

    this.currentPage = 0;
    this.mode = 'list';

    this.onFiltersChanged();
  }

  openView(t: UITransaction) { this.selected = { ...t }; this.modalMode = 'view'; }
  openDelete(t: UITransaction) { this.selected = { ...t }; this.modalMode = 'delete'; }

  confirmDelete(): void {
    if (!this.selected) { return; }

    const id = +this.selected.id.replace(/^p-/, '');
    const req$ = this.selected.planned
      ? this.planApi.delete(id)
      : this.txApi.delete(id);

    this.handleSave(req$, () => this.afterSave(), 'Transaction deleted');
  }

  private getLocation(): Promise<{ lat: number, lon: number } | null> {
    return new Promise(res => {
      navigator.geolocation.getCurrentPosition(
        p => res({ lat: +p.coords.latitude.toFixed(6), lon: +p.coords.longitude.toFixed(6) }),
        _ => res(null),
        { enableHighAccuracy: true, timeout: 10_000 }
      );
    });
  }

  openEdit(t: UITransaction): void {
    this.loadingEdit = true;
    this.selected = { ...t };

    const open = (data: any, mode: ModalMode) => {
      this.editForm = { ...data };
      this.modalMode = mode;
      this.loadingEdit = false;
    };

    if (t.planned) {
      open(
        {
          id: t.id.slice(2),
          planned: true,
          type: t.type,
          amount: t.amount,
          categoryId: t.categoryId,
          title: t.category,
          date: t.date.toISOString().slice(0, 10),
          note: '',
        },
        'planEdit'
      );
    } else if (t.hasFull) {
      open(
        {
          id: t.id,
          planned: false,
          type: t.type,
          amount: t.amount,
          categoryId: t.categoryId,
          date: t.date.toISOString().slice(0, 16),
          note: t.note || '',
        },
        'fullEdit'
      );
    } else {
      this.txApi.getById(+t.id).subscribe((r) => {
        const full = r.data;
        open(
          {
            id: String(full.id),
            planned: false,
            type: full.type === 'E' ? 'expense' : 'income',
            amount: +full.amount,
            categoryId: full.categoryId,
            date: new Date(full.txTime).toISOString().slice(0, 16),
            note: full.note || '',
          },
          'fullEdit'
        );
      });
    }
  }

  async saveEdit(frm: NgForm): Promise<void> {
    if (frm.invalid) return;

    if (this.modalMode === 'quickEdit' || this.modalMode === 'fullEdit') {
      const dto: FullTxRequestDTO = {
        type: this.editForm.type === 'expense' ? 'E' : 'I',
        amount: +this.editForm.amount,
        categoryId: +this.editForm.categoryId,
        txTime: new Date(this.editForm.date),
        note: this.editForm.note || '',
      };
      this.handleSave(
        this.txApi.patch(+this.editForm.id, dto),
        () => this.afterSave(),
        'Transaction updated'
      );
    } else {
      const dto: PlannedTxRequestDTO = {
        type: this.editForm.type === 'expense' ? 'E' : 'I',
        categoryId: +this.editForm.categoryId,
        title: this.editForm.title,
        amount: +this.editForm.amount,
        dueDate: new Date(this.editForm.date) as any,
      };
      this.handleSave(
        this.planApi.update(+this.editForm.id, dto),
        () => this.afterSave(),
        'Planned item updated'
      );
    }
  }

  addTx() {
    this.modalMode = 'newTx';
    this.editForm = {
      planned: false,
      type: 'expense', amount: 0, categoryId: null,
      date: new Date().toISOString().slice(0, 16),
      note: '', useGeo: false,
      latitude: null, longitude: null
    };
  }

  addPlanned() {
    this.modalMode = 'newPlan';
    this.editForm = {
      planned: true,
      type: 'expense', amount: 0, categoryId: null,
      title: '', date: new Date().toISOString().slice(0, 10),
      note: '', useGeo: false,
      latitude: null, longitude: null
    };
  }

  async saveNew(frm: NgForm) {
    if (frm.invalid) return;

    if (this.editForm.useGeo && (!this.editForm.latitude || !this.editForm.longitude)) {
      const pos = await this.getLocation();
      if (pos) { this.editForm.latitude = pos.lat; this.editForm.longitude = pos.lon; }
    }

    if (this.modalMode === 'newTx') {
      const dto: FullTxRequestDTO = {
        type: this.editForm.type === 'expense' ? 'E' : 'I',
        amount: +this.editForm.amount,
        categoryId: +this.editForm.categoryId,
        txTime: new Date(this.editForm.date),
        note: this.editForm.note || '',
        latitude: this.editForm.useGeo ? this.editForm.latitude : null,
        longitude: this.editForm.useGeo ? this.editForm.longitude : null
      };
      this.handleSave(this.txApi.create(dto), () => this.afterSave(), 'Transaction added');
    } else {
      const dto: PlannedTxRequestDTO = {
        type: this.editForm.type === 'expense' ? 'E' : 'I',
        categoryId: +this.editForm.categoryId,
        title: this.editForm.title,
        amount: +this.editForm.amount,
        dueDate: new Date(this.editForm.date) as any,
      };
      this.handleSave(this.planApi.create(dto), () => this.afterSave(), 'Planned item added');
    }
  }

  isEdit(): boolean { return ['quickEdit', 'fullEdit', 'planEdit'].includes(this.modalMode); }
  isNew(): boolean { return ['newTx', 'newPlan'].includes(this.modalMode); }

  afterSave() {
    this.closeModal();
    this.fetchPage(this.currentPage);
  }

  closeModal() {
    this.modalMode = 'view';
    this.selected = null;
    this.editForm = {};
    this.loadingEdit = false;
  }

  handlePdfChosen(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.handleSave(this.revApi.importPdf(file), () => this.fetchPage(this.currentPage), 'Revolut PDF imported');
    (e.target as HTMLInputElement).value = '';
    this.fabOpen = false;
  }
}
