
/* ================= Tiện ích ================= */
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
const money = (n) => new Intl.NumberFormat('vi-VN').format(n || 0) + ' ₫';
const dt = (s) => (s ? new Date(s).toLocaleString('vi-VN') : '');
const err = (m) => (m ? `<div class="msg error">${esc(m)}</div>` : '');
const okMsg = (m) => (m ? `<div class="msg success">${esc(m)}</div>` : '');
const draw = (html) => {
    const el = document.getElementById('content');
    if (el)
        el.innerHTML = html;
};

const val = (id) => {
    const el = document.getElementById(id);

    if (!el) {
        throw new Error('Không tìm thấy ô nhập có id: ' + id);
    }

    return el.value;
};


const STATUS = {PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối'};

let token = localStorage.getItem('token');
let user = JSON.parse(localStorage.getItem('user') || 'null');
const isAdmin = () => !!user && user.role === 'ADMIN';
const isManager = () => !!user && user.role === 'MANAGER';
const canManageWarehouse = () => isAdmin() || isManager();

function setSession(t, u) {
    token = t;
    user = u;
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify(u));
}
function clearSession() {
    token = null;
    user = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}

async function api(path, method = 'GET', body) {
    const headers = {'Content-Type': 'application/json'};
    if (token)
        headers.Authorization = 'Bearer ' + token;
    const res = await fetch('/api' + path, {method, headers, body: body !== undefined ? JSON.stringify(body) : undefined});
    const text = await res.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (e) {
        data = null;
    }
    if (res.status === 401 && path !== '/auth/login') {
        clearSession();
        location.hash = '#/login';
        render();
        throw new Error('Phiên đăng nhập đã hết hạn');
    }
    if (!res.ok) {
        console.error('API ERROR:', {
            status: res.status,
            statusText: res.statusText,
            data: data,
            raw: text
        });

        throw new Error(
                (data && data.message)
                || text
                || 'Có lỗi xảy ra (' + res.status + ')'
                );
    }
    return data;
}

/* ================= Khung trang & điều hướng ================= */
function layout() {
    const links = [
        ['#/', 'Tổng quan'],
        ['#/products', 'Sản phẩm'],
        ['#/transactions', 'Phiếu nhập/xuất'],
        ['#/lectures', '📚 Bài giảng']
    ];

    // ADMIN và MANAGER được dùng Danh mục + Nhà cung cấp
    if (isAdmin() || user?.role === 'MANAGER') {
        links.push(
                ['#/categories', 'Danh mục'],
                ['#/suppliers', 'Nhà cung cấp']
                );
    }

    // Chỉ ADMIN được thấy Người dùng
    if (isAdmin()) {
        links.push(['#/users', 'Người dùng']);
    }

    const here = location.hash || '#/';

    document.getElementById('app').innerHTML = `
    <header class="topbar">
      <div class="brand">Wings <b>Up</b> · Kho</div>

      <nav class="nav">
        ${links.map(([h, t]) =>
            `<a href="${h}" class="${here === h ? 'active' : ''}">${t}</a>`
    ).join('')}
      </nav>

      <div class="who">
        <a href="#/profile">
          ${esc(user.fullName || user.username)}
          (${esc(user.role)})
        </a>

        <a href="#" onclick="logout(); return false;">
          Đăng xuất
        </a>
      </div>
    </header>

    <main id="content"></main>
  `;
}

async function render() {
    if (!token)
        return pageLogin();

    const route = (location.hash || '#/').slice(2);

    // Chỉ ADMIN được vào trang Người dùng
    if (route === 'users' && !isAdmin()) {
        location.hash = '#/';
        return;
    }

    // ADMIN và MANAGER được vào Danh mục + Nhà cung cấp
    if (
            (route === 'categories' || route === 'suppliers')
            && !(isAdmin() || user?.role === 'MANAGER')
            ) {
        location.hash = '#/';
        return;
    }

    layout();

    if (CRUD[route]) {
        return pageCrud(route);
    }

    switch (route) {
        case 'products':
            return pageProducts();

        case 'transactions':
            return pageTransactions();

        case 'lectures':
            return pageLectures();

        case 'profile':
            return pageProfile();

        default:
            return pageDashboard();
    }
}

/* ================= Đăng nhập / Đăng ký ================= */

let registerMode = false;

function pageLogin(msg = '') {
    registerMode = false;

    document.getElementById('app').innerHTML = `
    <div class="login-page">
      <div class="login-box">
        <h1>Wings Up – Edu Success</h1>
        <p>Đăng nhập hệ thống quản lý kho</p>

        ${err(msg)}

        <label>
          Tên đăng nhập
          <input id="lu" autocomplete="username">
        </label>

        <label>
          Mật khẩu
          <input id="lp" type="password" autocomplete="current-password">
        </label>

        <button class="btn" onclick="doLogin()">Đăng nhập</button>

        <div class="register-link">
          Chưa có tài khoản?
          <button class="btn ghost" onclick="showRegister()">Đăng ký</button>
        </div>

        <div class="hint">
          Tài khoản mẫu: admin / admin123 · staff / staff123
        </div>
      </div>
    </div>`;

    document.getElementById('lp').addEventListener('keyup', (e) => {
        if (e.key === 'Enter')
            doLogin();
    });
}

function showRegister(msg = '') {
    registerMode = true;

    document.getElementById('app').innerHTML = `
    <div class="login-page">
      <div class="login-box">
        <h1>Wings Up – Edu Success</h1>
        <p>Tạo tài khoản mới</p>

        ${err(msg)}

        <label>
          Tên đăng nhập
          <input id="ru" autocomplete="username">
        </label>

        <label>
          Mật khẩu
          <input id="rp" type="password" autocomplete="new-password">
        </label>

        <label>
          Họ tên
          <input id="rf" autocomplete="name">
        </label>

        <label>
          Email
          <input id="re" type="email" autocomplete="email">
        </label>

        <button class="btn" onclick="doRegister()">
          Tạo tài khoản
        </button>

        <div class="register-link">
          Đã có tài khoản?
          <button class="btn ghost" onclick="pageLogin()">
            Đăng nhập
          </button>
        </div>
      </div>
    </div>`;

    document.getElementById('rp').addEventListener('keyup', (e) => {
        if (e.key === 'Enter')
            doRegister();
    });
}

async function doRegister() {
    const username = val('ru').trim();
    const password = val('rp');
    const fullName = val('rf').trim();
    const email = val('re').trim();

    if (!username) {
        showRegister('Tên đăng nhập không được để trống');
        return;
    }

    if (!password || password.length < 6) {
        showRegister('Mật khẩu phải từ 6 ký tự');
        return;
    }

    try {
        await api('/auth/register', 'POST', {
            username: username,
            password: password,
            fullName: fullName,
            email: email
        });

        pageLogin();
        alert('Tạo tài khoản thành công! Vui lòng đăng nhập.');
    } catch (e) {
        showRegister(e.message);
    }
}

async function doLogin() {
    try {
        const r = await api('/auth/login', 'POST', {
            username: val('lu'),
            password: val('lp')
        });

        setSession(r.token, r.user);
        location.hash = '#/';
        render();
    } catch (e) {
        pageLogin(e.message);
    }
}

async function logout() {
    try {
        await api('/auth/logout', 'POST');
    } catch (e) {
        console.log('Logout error:', e);
    }

    clearSession();
    location.hash = '#/login';
    render();
}

/* ================= Tổng quan ================= */
async function pageDashboard() {
    try {
        const d = await api('/dashboard');
        draw(`
      <h1>Tổng quan kho</h1>
      <div class="grid">
        <div class="stat"><span>Số loại sản phẩm</span><strong>${d.totalProducts}</strong></div>
        <div class="stat"><span>Tổng số lượng tồn</span><strong>${d.totalQuantity}</strong></div>
        <div class="stat"><span>Giá trị tồn kho</span><strong>${money(d.totalValue)}</strong></div>
        <div class="stat ${d.lowStockCount ? 'alert' : ''}"><span>Sắp hết hàng</span><strong>${d.lowStockCount}</strong></div>
        <div class="stat"><span>${isAdmin() ? 'Phiếu chờ duyệt' : 'Phiếu của tôi đang chờ'}</span><strong>${d.pendingCount}</strong></div>
      </div>
      <div class="panel">
        <h2>Sản phẩm dưới mức tồn tối thiểu</h2>
        ${d.lowStock.length ? `<div class="table-wrap"><table>
          <thead><tr><th>Mã</th><th>Tên</th><th class="num">Tồn</th><th class="num">Tối thiểu</th></tr></thead>
          <tbody>${d.lowStock.map((p) => `<tr><td>${esc(p.sku)}</td><td>${esc(p.name)}</td><td class="num low">${p.quantity}</td><td class="num">${p.minQuantity}</td></tr>`).join('')}</tbody>
        </table></div>` : '<div class="empty">Không có sản phẩm nào dưới mức tối thiểu.</div>'}
      </div>
      <div class="panel">
        <h2>${isAdmin() ? 'Phiếu gần đây' : 'Phiếu gần đây của tôi'}</h2>
        ${d.recent.length ? `<div class="table-wrap"><table>
          <thead><tr><th>Thời gian</th><th>Loại</th><th>Sản phẩm</th><th class="num">SL</th><th>Trạng thái</th></tr></thead>
          ${d.recent.map((t) => `
    <tr>
        <td>${dt(t.createdAt)}</td>

        <td>
            <span class="badge ${t.type}">
                ${t.type === 'IN' ? 'Nhập' : 'Xuất'}
            </span>
        </td>

        <td>
            ${esc(t.product?.name || 'Sản phẩm đã xóa')}
        </td>

        <td class="num">
            ${t.quantity}
        </td>

        <td>
            <span class="badge ${t.status}">
                ${STATUS[t.status] || t.status}
            </span>
        </td>
    </tr>
`).join('')}</tbody>
        </table></div>` : '<div class="empty">Chưa có phiếu nào.</div>'}
      </div>`);
    } catch (e) {
        draw(`<h1>Tổng quan kho</h1>${err(e.message)}`);
    }
}

/* ================= Sản phẩm ================= */
let prod = {};

async function pageProducts() {
    prod = {items: [], cats: [], sups: [], q: '', form: null, editing: null, error: ''};
    try {
        prod.cats = await api('/categories');
        prod.sups = await api('/suppliers');
    } catch (e) {
        prod.error = e.message;
    }
    prodLoad();
}

async function prodLoad() {
    try {
        prod.items = await api('/products' + (prod.q ? '?q=' + encodeURIComponent(prod.q) : ''));
    } catch (e) {
        prod.error = e.message;
    }
    prodDraw();
}

function prodSearch() {
    prod.q = val('q').trim();
    prod.error = '';
    prodLoad();
}

function prodAdd() {
    prod.editing = null;
    prod.error = '';
    prod.form = {sku: '', name: '', unit: '', price: 0, quantity: 0, minQuantity: 0, categoryId: '', supplierId: ''};
    prodDraw();
}

function prodEdit(id) {
    const p = prod.items.find((x) => x.id === id);
    prod.editing = id;
    prod.error = '';
    prod.form = {
        sku: p.sku, name: p.name, unit: p.unit || '', price: p.price, minQuantity: p.minQuantity,
        categoryId: p.category ? p.category.id : '', supplierId: p.supplier ? p.supplier.id : ''
    };
    prodDraw();
}

function prodCancel() {
    prod.form = null;
    prod.error = '';
    prodDraw();
}

async function prodSave() {
    try {
        const f = {
            name: val('p_name').trim(),
            unit: val('p_unit'),
            price: Number(val('p_price')) || 0,
            minQuantity: parseInt(val('p_min')) || 0,
            quantity: prod.editing
                ? 0
                : parseInt(val('p_qty')) || 0,
            categoryId: val('p_cat'),
            supplierId: val('p_sup')
        };

        console.log('DỮ LIỆU GỬI:', f);



        if (!f.name) {
            alert('Vui lòng nhập Tên sản phẩm');
            return;
        }

        const body = {
            name: f.name,
            unit: f.unit,
            price: f.price,
            quantity: f.quantity,
            minQuantity: f.minQuantity,
            category: f.categoryId
                ? { id: Number(f.categoryId) }
                : null,
            supplier: f.supplierId
                ? { id: Number(f.supplierId) }
                : null
        };

        console.log('BODY GỬI SERVER:', body);

        const result = await api(
            prod.editing
                ? '/products/' + prod.editing
                : '/products',
            prod.editing ? 'PUT' : 'POST',
            body
        );

        console.log('SERVER TRẢ VỀ:', result);

        alert(
            prod.editing
                ? 'Đã sửa sản phẩm thành công!'
                : 'Đã thêm sản phẩm thành công! ID = ' + result.id
        );

        prod.form = null;
        prod.error = '';

        await prodLoad();

    } catch (e) {
        console.error('LỖI LƯU SẢN PHẨM:', e);
        alert('Lỗi: ' + e.message);

        prod.error = e.message;
        prodDraw();
    }
}



async function prodDelete(id) {
    if (!confirm('Bạn có chắc muốn xóa sản phẩm này?'))
        return;

    try {
        await api('/products/' + id, 'DELETE');

        alert('✅ Đã xóa sản phẩm thành công!');

        await prodLoad();
    } catch (e) {
        alert('❌ ' + e.message);
    }
}

function prodFormHtml() {
    const f = prod.form;
    const opt = (list, selected) => '<option value="">— Chưa chọn —</option>' +
                list.map((x) => `<option value="${x.id}" ${String(x.id) === String(selected) ? 'selected' : ''}>${esc(x.name)}</option>`).join('');
    return `<div class="panel">
    <h2>${prod.editing ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}</h2>
    <div class="form">
      <label>Tên sản phẩm <input id="p_name" value="${esc(f.name)}"></label>
      <label>Đơn vị <input id="p_unit" value="${esc(f.unit)}"></label>
      <label>Giá <input id="p_price" type="number" min="0" value="${esc(f.price)}"></label>
      ${prod.editing ? '' : `<label>Tồn ban đầu <input id="p_qty" type="number" min="0" value="${esc(f.quantity)}"></label>`}
      <label>Tồn tối thiểu <input id="p_min" type="number" min="0" value="${esc(f.minQuantity)}"></label>
      <label>Danh mục <select id="p_cat">${opt(prod.cats, f.categoryId)}</select></label>
      <label>Nhà cung cấp <select id="p_sup">${opt(prod.sups, f.supplierId)}</select></label>
    </div>
    <div class="actions" style="margin-top:12px">
      <button class="btn" onclick="prodSave()">Lưu sản phẩm</button>
      <button class="btn ghost" onclick="prodCancel()">Hủy</button>
    </div>
  </div>`;
}

function prodDraw() {
    const admin = isAdmin();
    const manager = user?.role === 'MANAGER';
    const canManage = admin || manager;

    draw(`
    <h1>Sản phẩm</h1>
    ${err(prod.error)}

    <div class="toolbar">
      <input id="q"
             placeholder="Tìm theo mã hoặc tên sản phẩm"
             value="${esc(prod.q)}">

      <button class="btn ghost" onclick="prodSearch()">Tìm</button>

      ${canManage
            ? '<button class="btn" onclick="prodAdd()">Thêm sản phẩm</button>'
            : ''
            }
    </div>

    ${prod.form ? prodFormHtml() : ''}

    <div class="panel table-wrap">
      ${
            prod.items.length
            ? `
            <table>
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Tên</th>
                  <th>Danh mục</th>
                  <th>Nhà cung cấp</th>
                  <th>Đơn vị</th>
                  <th class="num">Giá</th>
                  <th class="num">Tồn</th>
                  <th class="num">Tối thiểu</th>
                  ${canManage ? '<th></th>' : ''}
                </tr>
              </thead>

              <tbody>
                ${prod.items.map((p) => `
                  <tr>
                    <td>${p.id}</td>
                    <td>${esc(p.name)}</td>
                    <td>${esc(p.category ? p.category.name : '')}</td>
                    <td>${esc(p.supplier ? p.supplier.name : '')}</td>
                    <td>${esc(p.unit)}</td>
                    <td class="num">${money(p.price)}</td>
                    <td class="num ${p.quantity <= p.minQuantity ? 'low' : ''}">
                      ${p.quantity}
                    </td>
                    <td class="num">${p.minQuantity}</td>

                    ${
                        canManage
                        ? `
                          <td>
                            <div class="actions">
                              <button
                                class="btn ghost small"
                                onclick="prodEdit(${p.id})">
                                Sửa
                              </button>

                              <button
                                class="btn danger small"
                                onclick="prodDelete(${p.id})">
                                Xóa
                              </button>
                            </div>
                          </td>
                        `
                        : ''
                        }
                  </tr>
                `).join('')}
              </tbody>
            </table>
          `
            : '<div class="empty">Chưa có sản phẩm nào.</div>'
            }
    </div>
  `);

    const q = document.getElementById('q');

    if (q) {
        q.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                prodSearch();
            }
        });
    }
}

/* ================= Phiếu nhập/xuất ================= */
let tx = {};

async function pageTransactions() {
    tx = {items: [], products: [], form: {productId: '', type: 'IN', quantity: 1, note: ''}, error: '', success: ''};
    txLoad();
}

async function txLoad() {
    try {
        tx.items = await api('/transactions');
        tx.products = await api('/products');
    } catch (e) {
        tx.error = e.message;
    }
    txDraw();
}

async function txCreate() {
    tx.form = {productId: val('t_prod'), type: val('t_type'), quantity: parseInt(val('t_qty')) || 0, note: val('t_note')};
    tx.error = '';
    tx.success = '';
    try {
        await api('/transactions', 'POST', tx.form);
        tx.success = canManageWarehouse()
                ? 'Đã tạo phiếu và cập nhật tồn kho'
                : 'Đã gửi phiếu, chờ quản trị viên duyệt';
        tx.form = {productId: '', type: tx.form.type, quantity: 1, note: ''};
        txLoad();
    } catch (e) {
        tx.error = e.message;
        txDraw();
    }
}
async function txCreate() {
    tx.form = {
        productId: val('t_prod'),
        type: val('t_type'),
        quantity: parseInt(val('t_qty')) || 0,
        note: val('t_note')
    };

    tx.error = '';
    tx.success = '';

    // USER chỉ được order tối đa 10 sản phẩm
    if (user?.role === 'USER' && tx.form.quantity > 10) {
        tx.error = 'Người dùng chỉ được order tối đa 10 sản phẩm mỗi phiếu.';
        txDraw();
        return;
    }

    // Số lượng phải lớn hơn 0
    if (tx.form.quantity < 1) {
        tx.error = 'Số lượng phải lớn hơn 0.';
        txDraw();
        return;
    }

    try {
        await api('/transactions', 'POST', tx.form);

        tx.success = canManageWarehouse()
            ? 'Đã tạo phiếu và cập nhật tồn kho'
            : 'Đã gửi phiếu, chờ quản trị viên duyệt';

        tx.form = {
            productId: '',
            type: tx.form.type,
            quantity: 1,
            note: ''
        };

        txLoad();

    } catch (e) {
        tx.error = e.message;
        txDraw();
    }
}


async function txAct(path, method, message) {
    tx.error = '';
    tx.success = '';
    try {
        await api(path, method);
        tx.success = message;
    } catch (e) {
        tx.error = e.message;
    }
    txLoad();
}

const txApprove = (id) => txAct('/transactions/' + id + '/approve', 'POST', 'Đã duyệt phiếu');
const txReject = (id) => txAct('/transactions/' + id + '/reject', 'POST', 'Đã từ chối phiếu');
const txCancel = (id) => {
    if (confirm('Hủy phiếu này?'))
        txAct('/transactions/' + id, 'DELETE', 'Đã hủy phiếu');
};
const txCanCancel = (t) =>
    t.status === 'PENDING'
            && (
                    isAdmin()
                    || isManager()
                    || (t.createdBy && t.createdBy.id === user.id)
                    );

function txDraw() {
    const f = tx.form;
    draw(`
    <h1>Phiếu nhập/xuất kho</h1>
    ${err(tx.error)}${okMsg(tx.success)}
    <div class="panel">
      <h2>Tạo phiếu mới</h2>
      <div class="form">
        <label>Loại phiếu
          <select id="t_type">
            <option value="IN" ${f.type === 'IN' ? 'selected' : ''}>Nhập kho</option>
            <option value="OUT" ${f.type === 'OUT' ? 'selected' : ''}>Xuất kho</option>
          </select></label>
        <label>Sản phẩm
          <select id="t_prod"><option value="">— Chọn sản phẩm —</option>
            ${tx.products.map((p) => `<option value="${p.id}" ${String(p.id) === String(f.productId) ? 'selected' : ''}>${esc(p.sku)} – ${esc(p.name)} (tồn ${p.quantity})</option>`).join('')}
          </select></label>
        <label>Số lượng <input id="t_qty" type="number" min="1" value="${esc(f.quantity)}"></label>
        <label>Ghi chú <input id="t_note" value="${esc(f.note)}"></label>
        <button class="btn" onclick="txCreate()">Tạo phiếu</button>
      </div>
    </div>
    <div class="panel table-wrap">
      ${tx.items.length ? `<table>
        <thead><tr><th>Thời gian</th><th>Loại</th><th>Sản phẩm</th><th class="num">SL</th><th>Người tạo</th><th>Ghi chú</th><th>Trạng thái</th><th></th></tr></thead>
        <tbody>${tx.items.map((t) => `<tr>
  <td>${dt(t.createdAt)}</td>

  <td>
    <span class="badge ${t.type}">
      ${t.type === 'IN' ? 'Nhập' : 'Xuất'}
    </span>
  </td>

  <td>
    ${esc(t.product?.name || 'Sản phẩm đã xóa')}
  </td>

  <td class="num">${t.quantity}</td>

  <td>
    ${esc(t.createdBy
                ? (t.createdBy.fullName || t.createdBy.username)
                : '')}
  </td>

  <td>${esc(t.note || '')}</td>

  <td>
    <span class="badge ${t.status}">
      ${STATUS[t.status] || t.status}
    </span>
  </td>

  <td>
    <div class="actions">
      ${canManageWarehouse() && t.status === 'PENDING' ? `
        <button
          class="btn ok small"
          onclick="txApprove(${t.id})">
          Duyệt
        </button>

        <button
          class="btn danger small"
          onclick="txReject(${t.id})">
          Từ chối
        </button>
      ` : ''}

      ${txCanCancel(t) ? `
        <button
          class="btn ghost small"
          onclick="txCancel(${t.id})">
          Hủy phiếu
        </button>
      ` : ''}
    </div>
  </td>

</tr>`).join('')}</tbody></table>` : '<div class="empty">Chưa có phiếu nào.</div>'}
    </div>`);
}

/* ================= Danh mục / Nhà cung cấp / Người dùng (dùng chung) ================= */
const CRUD = {
    categories: {
        title: 'Danh mục', endpoint: '/categories',
        fields: [{key: 'name', label: 'Tên danh mục'}, {key: 'description', label: 'Mô tả'}]
    },
    suppliers: {
        title: 'Nhà cung cấp', endpoint: '/suppliers',
        fields: [
            {key: 'name', label: 'Tên nhà cung cấp'}, {key: 'phone', label: 'Số điện thoại'},
            {key: 'email', label: 'Email'}, {key: 'address', label: 'Địa chỉ'}
        ]
    },
    users: {
        title: 'Người dùng', endpoint: '/users',
        fields: [
            {key: 'username', label: 'Tên đăng nhập', lockOnEdit: true},
            {key: 'password', label: 'Mật khẩu', type: 'password', hideInTable: true, hint: 'Để trống khi sửa nếu không đổi'},
            {key: 'fullName', label: 'Họ tên'}, {key: 'email', label: 'Email'},
            {key: 'role', label: 'Vai trò', type: 'select', options: ['USER', 'ADMIN', 'MANAGER']},
            {key: 'active', label: 'Đang hoạt động', type: 'checkbox'}
        ]
    }
};

let crud = null;

async function pageCrud(key) {
    crud = {key, cfg: CRUD[key], items: [], editing: null, values: null, error: ''};
    crudLoad();
}

async function crudLoad() {
    try {
        crud.items = await api(crud.cfg.endpoint);
    } catch (e) {
        crud.error = e.message;
    }
    crudDraw();
}

function crudBlank() {
    const v = {};
    crud.cfg.fields.forEach((f) => {
        v[f.key] = f.type === 'checkbox' ? true : f.options ? f.options[0] : '';
    });
    return v;
}

function crudAdd() {
    crud.editing = null;
    crud.error = '';
    crud.values = crudBlank();
    crudDraw();
}

function crudEdit(id) {
    const it = crud.items.find((x) => x.id === id);
    crud.editing = id;
    crud.error = '';
    crud.values = {...crudBlank(), ...it};
    crud.cfg.fields.filter((f) => f.type === 'password').forEach((f) => {
        crud.values[f.key] = '';
    });
    crudDraw();
}

function crudCancel() {
    crud.values = null;
    crud.error = '';
    crudDraw();
}

async function crudSave() {
    const v = {};
    crud.cfg.fields.forEach((f) => {
        const el = document.getElementById('f_' + f.key);
        v[f.key] = f.type === 'checkbox' ? el.checked : el.value;
    });
    crud.values = v;
    try {
        await api(crud.editing ? crud.cfg.endpoint + '/' + crud.editing : crud.cfg.endpoint, crud.editing ? 'PUT' : 'POST', v);
        crud.values = null;
        crud.error = '';
        crudLoad();
    } catch (e) {
        crud.error = e.message;
        crudDraw();
    }
}

async function crudDelete(id) {
    const typeName = {
        categories: 'danh mục',
        suppliers: 'nhà cung cấp'
    };

    const type = crud.cfg.endpoint.replace('/api/', '');
    const name = typeName[type] || 'mục này';

    if (!confirm(`Bạn có chắc muốn xóa ${name} này?`)) {
        return;
    }

    try {
        await api(
                crud.cfg.endpoint + '/' + id,
                'DELETE'
                );

        crud.error = '';

        await crudLoad();

        alert(`✅ Đã xóa ${name} thành công!`);

    } catch (e) {
        console.error('LỖI XÓA:', e);

        alert('❌ Lỗi xóa: ' + e.message);

        crud.error = e.message;

        crudDraw();
    }
}

function crudFormHtml() {
    const v = crud.values;
    const inputs = crud.cfg.fields.map((f) => {
        const id = 'f_' + f.key;
        if (f.type === 'checkbox')
            return `<label class="check"><input type="checkbox" id="${id}" ${v[f.key] ? 'checked' : ''}> ${esc(f.label)}</label>`;
        if (f.type === 'select')
            return `<label>${esc(f.label)}<select id="${id}">${f.options.map((o) => `<option value="${o}" ${v[f.key] === o ? 'selected' : ''}>${o}</option>`).join('')}</select></label>`;
        const disabled = f.lockOnEdit && crud.editing ? 'disabled' : '';
        return `<label>${esc(f.label)}<input id="${id}" type="${f.type || 'text'}" value="${esc(v[f.key])}" placeholder="${esc(f.hint || '')}" ${disabled}></label>`;
    }).join('');
    return `<div class="panel"><div class="form">${inputs}</div>
    <div class="actions" style="margin-top:12px">
      <button class="btn" onclick="crudSave()">Lưu</button>
      <button class="btn ghost" onclick="crudCancel()">Hủy</button>
    </div></div>`;
}

function crudDraw() {
    const shown = crud.cfg.fields.filter((f) => !f.hideInTable);
    draw(`
    <h1>${crud.cfg.title}</h1>
    ${err(crud.error)}
    <div class="toolbar"><button class="btn" onclick="crudAdd()">Thêm mới</button></div>
    ${crud.values ? crudFormHtml() : ''}
    <div class="panel table-wrap">
      ${crud.items.length ? `<table>
        <thead><tr>${shown.map((f) => `<th>${esc(f.label)}</th>`).join('')}<th></th></tr></thead>
        <tbody>${crud.items.map((it) => `<tr>
          ${shown.map((f) => `<td>${f.type === 'checkbox' ? (it[f.key] ? 'Có' : 'Không') : esc(it[f.key])}</td>`).join('')}
          <td><div class="actions">
            <button class="btn ghost small" onclick="crudEdit(${it.id})">Sửa</button>
            <button class="btn danger small" onclick="crudDelete(${it.id})">Xóa</button>
          </div></td></tr>`).join('')}</tbody></table>` : '<div class="empty">Chưa có dữ liệu.</div>'}
    </div>`);
}


/* ================= Bài giảng ================= */

let lectures = {
    items: [],
    form: null,
    editing: null,
    error: ''
};

async function pageLectures() {
    lectures = {
        items: [],
        form: null,
        editing: null,
        error: ''
    };

    await lectureLoad();
}

async function lectureLoad() {
    try {
        lectures.items = await api('/lectures');
        lectures.error = '';
    } catch (e) {
        lectures.error = e.message;
    }

    lectureDraw();
}

function lectureAdd() {
    lectures.editing = null;
    lectures.error = '';

    lectures.form = {
        name: '',
        link: ''
    };

    lectureDraw();
}

function lectureEdit(id) {
    const item = lectures.items.find(x => x.id === id);

    if (!item)
        return;

    lectures.editing = id;
    lectures.error = '';

    lectures.form = {
        name: item.name || '',
        link: item.link || ''
    };

    lectureDraw();
}

function lectureCancel() {
    lectures.form = null;
    lectures.editing = null;
    lectures.error = '';

    lectureDraw();
}

async function lectureSave() {

    const name = val('lecture_name').trim();
    const link = val('lecture_link').trim();

    if (!name) {
        lectures.error = 'Tên bài giảng không được để trống';
        lectureDraw();
        return;
    }

    if (!link) {
        lectures.error = 'Link bài giảng không được để trống';
        lectureDraw();
        return;
    }

    if (!/^https?:\/\/.+/i.test(link)) {
        lectures.error = 'Link phải bắt đầu bằng http:// hoặc https://';
        lectureDraw();
        return;
    }

    const body = {
        name: name,
        link: link
    };

    try {

        if (lectures.editing) {

            await api(
                    '/lectures/' + lectures.editing,
                    'PUT',
                    body
                    );

            alert('Đã sửa bài giảng!');

        } else {

            await api(
                    '/lectures',
                    'POST',
                    body
                    );

            alert('Đã thêm bài giảng!');
        }

        lectures.form = null;
        lectures.editing = null;
        lectures.error = '';

        await lectureLoad();

    } catch (e) {

        lectures.error = e.message;
        lectureDraw();
    }
}

async function lectureDelete(id) {

    if (!confirm('Bạn có chắc muốn xóa bài giảng này?')) {
        return;
    }

    try {

        await api(
                '/lectures/' + id,
                'DELETE'
                );

        alert('Đã xóa bài giảng!');

        await lectureLoad();

    } catch (e) {

        lectures.error = e.message;
        lectureDraw();
    }
}

function lectureOpen(link) {

    if (!link)
        return;

    window.open(
            link,
            '_blank',
            'noopener,noreferrer'
            );
}

function lectureFormHtml() {

    const f = lectures.form;

    return `
        <div class="panel">

            <h2>
                ${lectures.editing
            ? 'Sửa bài giảng'
            : 'Thêm bài giảng'}
            </h2>

            <div class="form">

                <label>
                    Tên bài giảng

                    <input
                        id="lecture_name"
                        value="${esc(f.name)}"
                        placeholder="Ví dụ: Java cơ bản"
                    >
                </label>

                <label>
                    Link bài giảng

                    <input
                        id="lecture_link"
                        type="url"
                        value="${esc(f.link)}"
                        placeholder="https://..."
                    >
                </label>

            </div>

            <div
                class="actions"
                style="margin-top:12px"
            >

                <button
                    class="btn"
                    onclick="lectureSave()"
                >
                    Lưu bài giảng
                </button>

                <button
                    class="btn ghost"
                    onclick="lectureCancel()"
                >
                    Hủy
                </button>

            </div>

        </div>
    `;
}

function lectureDraw() {

    const canManage = isAdmin() || isManager();

    draw(`

        <h1>📚 Bài giảng</h1>

        ${err(lectures.error)}

        <div class="toolbar">

            ${
            canManage
            ? `
                        <button
                            class="btn"
                            onclick="lectureAdd()"
                        >
                            + Thêm bài giảng
                        </button>
                    `
            : ''
            }

        </div>

        ${
            lectures.form
            ? lectureFormHtml()
            : ''
            }

        <div class="panel table-wrap">

            ${
            lectures.items.length

            ? `

                        <table>

                            <thead>

                                <tr>
                                    <th>#</th>
                                    <th>Tên bài giảng</th>
                                    <th>Link</th>
                                    ${
            canManage
            ? '<th>Thao tác</th>'
            : ''
            }
                                </tr>

                            </thead>

                            <tbody>

                                ${
            lectures.items
            .map((item, index) => `

                                            <tr>

                                                <td>
                                                    ${index + 1}
                                                </td>

                                                <td>
                                                    <b>
                                                        ${esc(item.name)}
                                                    </b>
                                                </td>

                                                <td>

                                                    <button
                                                        class="btn small"
                                                        onclick="lectureOpen('${esc(item.link)}')"
                                                    >
                                                        🔗 Mở bài giảng
                                                    </button>

                                                </td>

                                                ${
                        canManage
                        ? `

                                                            <td>

                                                                <div
                                                                    class="actions"
                                                                >

                                                                    <button
                                                                        class="btn ghost small"
                                                                        onclick="lectureEdit(${item.id})"
                                                                    >
                                                                        Sửa
                                                                    </button>

                                                                    <button
                                                                        class="btn danger small"
                                                                        onclick="lectureDelete(${item.id})"
                                                                    >
                                                                        Xóa
                                                                    </button>

                                                                </div>

                                                            </td>

                                                        `
                        : ''
                        }

                                            </tr>

                                        `)
            .join('')
            }

                            </tbody>

                        </table>

                    `

            : `

                        <div class="empty">

                            Chưa có bài giảng nào.

                        </div>

                    `
            }

        </div>

    `);
}


/* ================= Tài khoản ================= */
function pageProfile(msg = '', isError = false) {
    draw(`
    <h1>Tài khoản</h1>
    <div class="panel"><p><b>${esc(user.fullName)}</b> · ${esc(user.username)} · ${esc(user.role)}</p></div>
    <div class="panel">
      <h2>Đổi mật khẩu</h2>
      ${isError ? err(msg) : okMsg(msg)}
      <div class="form">
        <label>Mật khẩu cũ <input id="pw_old" type="password"></label>
        <label>Mật khẩu mới (từ 6 ký tự) <input id="pw_new" type="password"></label>
        <button class="btn" onclick="changePassword()">Đổi mật khẩu</button>
      </div>
    </div>`);
}

async function changePassword() {
    try {
        await api('/auth/password', 'PUT', {oldPassword: val('pw_old'), newPassword: val('pw_new')});
        pageProfile('Đã đổi mật khẩu');
    } catch (e) {
        pageProfile(e.message, true);
    }
}

/* ================= Khởi động ================= */
window.addEventListener('hashchange', render);
render();