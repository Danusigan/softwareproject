"use client"

export default function Footer() {
  return (
    <footer className="bg-blue-700 text-white mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="font-bold mb-2">Postal Address</h3>
            <p className="text-sm">Faculty of Engineering,</p>
            <p className="text-sm">University of Ruhuna,</p>
            <p className="text-sm">Hapugala, Galle,</p>
            <p className="text-sm">Sri Lanka.</p>
            <p className="text-sm">80000</p>
          </div>
          <div>
            <h3 className="font-bold mb-2">Phone Number</h3>
            <p className="text-sm">+(94) 912245765,</p>
            <p className="text-sm">+(94) 912245766,</p>
            <p className="text-sm">+(94) 912245767</p>
          </div>
          <div>
            <h3 className="font-bold mb-2">Other</h3>
            <p className="text-sm">Fax : +94 912245762</p>
            <p className="text-sm">Email : ar@eng.ruh.ac.lk</p>
          </div>
        </div>
      </div>
    </footer>
  )
}
