import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router'
import { RequireRole } from '@/auth/RequireRole'
import { AppLayout } from '@/components/layout/AppLayout'
import { HomePage } from '@/pages/public/HomePage'
import { EventsPage } from '@/pages/public/EventsPage'
import { EventDetailPage } from '@/pages/public/EventDetailPage'
import { AboutPage } from '@/pages/public/AboutPage'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { CheckoutPage } from '@/pages/customer/CheckoutPage'
import { MyTicketsPage } from '@/pages/customer/MyTicketsPage'
import { MyOrdersPage } from '@/pages/customer/MyOrdersPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PageSpinner } from '@/components/ui/Spinner'

// Organizer, gate and admin areas pull in charts and the QR scanner; load them only when visited.
const DashboardPage = lazy(() => import('@/pages/organizer/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const VenuesPage = lazy(() => import('@/pages/organizer/VenuesPage').then((m) => ({ default: m.VenuesPage })))
const NewEventPage = lazy(() => import('@/pages/organizer/NewEventPage').then((m) => ({ default: m.NewEventPage })))
const ManageEventPage = lazy(() => import('@/pages/organizer/ManageEventPage').then((m) => ({ default: m.ManageEventPage })))
const EventStatsPage = lazy(() => import('@/pages/organizer/EventStatsPage').then((m) => ({ default: m.EventStatsPage })))
const ScannerPage = lazy(() => import('@/pages/gate/ScannerPage').then((m) => ({ default: m.ScannerPage })))
const AdminPage = lazy(() => import('@/pages/admin/AdminPage').then((m) => ({ default: m.AdminPage })))

export function App() {
  return (
    <Suspense fallback={<PageSpinner />}>
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="events" element={<EventsPage />} />
        <Route path="events/:id" element={<EventDetailPage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />

        <Route path="checkout" element={<RequireRole role="CUSTOMER"><CheckoutPage /></RequireRole>} />
        <Route path="my/tickets" element={<RequireRole role="CUSTOMER"><MyTicketsPage /></RequireRole>} />
        <Route path="my/orders" element={<RequireRole role="CUSTOMER"><MyOrdersPage /></RequireRole>} />

        <Route path="organizer" element={<RequireRole role="ORGANIZER"><DashboardPage /></RequireRole>} />
        <Route path="organizer/venues" element={<RequireRole role="ORGANIZER"><VenuesPage /></RequireRole>} />
        <Route path="organizer/events/new" element={<RequireRole role="ORGANIZER"><NewEventPage /></RequireRole>} />
        <Route path="organizer/events/:id" element={<RequireRole role="ORGANIZER"><ManageEventPage /></RequireRole>} />
        <Route path="organizer/events/:id/stats" element={<RequireRole role="ORGANIZER"><EventStatsPage /></RequireRole>} />

        <Route path="gate" element={<RequireRole role="GATE_STAFF"><ScannerPage /></RequireRole>} />
        <Route path="admin" element={<RequireRole role="ADMIN"><AdminPage /></RequireRole>} />

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
    </Suspense>
  )
}
