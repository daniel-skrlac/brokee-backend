import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
    ServiceResponseDTO,
    PagedResponseDTO,
    TxResponseDTO,
    FullTxRequestDTO,
    QuickTxRequestDTO,
} from '../api/dtos';
import { environment } from '../environments/environment';

@Injectable({ providedIn: 'root' })
export class TransactionService {
    private readonly base = `${environment.apiUrl}/transactions`;

    constructor(private http: HttpClient) { }

    page(
        page = 0,
        size = 10,
        opts?: {
            type?: 'expense' | 'income' | 'E' | 'I';
            min?: number;
            max?: number;
            from?: string;
            to?: string;
            note?: string;
            category?: string;
        }
    ): Observable<ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>>> {
        let params = new HttpParams().set('page', page).set('size', size);

        if (opts) {
            const t = opts.type === 'expense' ? 'E' : opts.type === 'income' ? 'I' : opts.type;
            if (t) params = params.set('type', t);
            if (opts.min != null) params = params.set('min', opts.min);
            if (opts.max != null) params = params.set('max', opts.max);
            if (opts.from) params = params.set('dueFrom', opts.from);
            if (opts.to) params = params.set('dueTo', opts.to);
            if (opts.note) params = params.set('note', opts.note);
            if (opts.category) params = params.set('category', opts.category);
        }

        return this.http.get<ServiceResponseDTO<PagedResponseDTO<TxResponseDTO>>>(this.base, { params });
    }

    recent(limit = 5): Observable<ServiceResponseDTO<TxResponseDTO[]>> {
        const params = new HttpParams().set('limit', limit);
        return this.http.get<ServiceResponseDTO<TxResponseDTO[]>>(
            `${this.base}/recent`,
            { params },
        );
    }

    create(dto: FullTxRequestDTO) {
        return this.http.post<ServiceResponseDTO<TxResponseDTO>>(this.base, dto);
    }

    quickAdd(dto: QuickTxRequestDTO) {
        return this.http.post<ServiceResponseDTO<TxResponseDTO>>(
            `${this.base}/quick`,
            dto,
        );
    }

    update(id: number, dto: FullTxRequestDTO) {
        return this.http.patch<ServiceResponseDTO<TxResponseDTO>>(
            `${this.base}/${id}`,
            dto,
        );
    }

    delete(id: number) {
        return this.http.delete<ServiceResponseDTO<boolean>>(
            `${this.base}/${id}`,
        );
    }

    getById(id: number) {
        return this.http.get<ServiceResponseDTO<TxResponseDTO>>(`${this.base}/${id}`);
    }

    patch(id: number, dto: FullTxRequestDTO) {
        return this.http.patch<ServiceResponseDTO<TxResponseDTO>>(`${this.base}/${id}`, dto);
    }
}
