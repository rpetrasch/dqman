import { TestBed } from '@angular/core/testing';

import { DqRule } from './dq-rule';

describe('DqRule', () => {
  let service: DqRule;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DqRule);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
